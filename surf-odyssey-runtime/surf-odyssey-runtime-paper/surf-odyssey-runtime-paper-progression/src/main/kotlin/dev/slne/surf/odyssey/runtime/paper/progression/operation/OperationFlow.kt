package dev.slne.surf.odyssey.runtime.paper.progression.operation

import dev.slne.surf.odyssey.core.common.progression.NewDeliveryDto
import dev.slne.surf.odyssey.core.common.progression.OperationDto
import dev.slne.surf.odyssey.core.common.progression.OperationResultDto
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.runtime.paper.progression.StackCodec
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.QuestReward
import dev.slne.surf.odyssey.runtime.paper.progression.delivery.Deliveries
import kotlinx.coroutines.CancellationException
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * What one operation does for a player.
 *
 * @property take the items held in escrow while the operation is committed and consumed when it is stored
 * @property changes the state changes to store, before the changes of [rewards]
 * @property rewards the rewards to give when the operation is stored
 * @property source a short identifier of the feature running the operation, stored with its deliveries
 */
data class OperationPlan(
    val take: List<Ingredient>,
    val changes: List<StateChange>,
    val rewards: List<QuestReward>,
    val source: String
)

/** How an operation ended. */
sealed interface OperationOutcome {
    /** The operation was stored; its items were consumed and its rewards given. */
    data object Success : OperationOutcome

    /** Another operation of the player was running, so nothing happened. */
    data object Busy : OperationOutcome

    /** The player lacked items of [OperationPlan.take], so nothing happened. */
    data object MissingItems : OperationOutcome

    /** The progression service could not be reached; the items held in escrow were given back. */
    data object Unavailable : OperationOutcome

    /**
     * The progression service refused the operation; the items held in escrow were given back.
     *
     * @property reason the reason the service gave
     */
    data class Rejected(val reason: String) : OperationOutcome
}

/** The calls of the progression service an operation needs. */
interface OperationBackend {
    /**
     * Commits [operation].
     *
     * @return the outcome of the commit
     */
    suspend fun commit(operation: OperationDto): OperationResultDto

    /**
     * Claims a delivery of [player].
     *
     * @return the encoded item for the winning claim, or `null` if the delivery is gone
     */
    suspend fun claim(player: UUID, deliveryId: UUID): String?
}

/** What an operation reports to and needs from the server it runs on. */
interface OperationHooks {
    /** Returns whether the progression service is considered reachable. */
    fun isAvailable(): Boolean

    /**
     * Called when a call to the progression service failed or timed out.
     *
     * @param action what was attempted
     * @param error the failure
     */
    fun onServiceFailure(action: String, error: Throwable)

    /**
     * Called when an operation of [player] is stored, before its rewards are handed out.
     *
     * @param changes the stored changes
     */
    fun onCommitted(player: UUID, changes: List<StateChange>)

    /** Called when items of [player] were left in or put into the pickup box. */
    fun onLeftInPickupBox(player: UUID)

    /**
     * Drops [items] at the feet of [player], because neither the inventory nor the pickup box could take them.
     *
     * @param items the stacks to drop
     */
    fun dropAtFeet(player: UUID, items: List<ItemStack>)
}

/**
 * Runs operations: holds the items to consume in escrow, commits the operation and then either hands out the rewards
 * or gives the escrow back.
 *
 * Only one operation per player runs at a time. Items that do not fit into the inventory go to the pickup box, and
 * items the pickup box cannot take either are dropped at the player's feet.
 *
 * @param backend the progression service
 * @param codec encodes the items of deliveries
 * @param planRewards turns the rewards of a plan into state changes and item stacks
 * @param hooks the server the operations run on
 * @param currency pays currency rewards after the operation is stored
 * @param commitTimeout how long a commit may take before it counts as failed
 */
class OperationFlow(
    backend: OperationBackend,
    private val codec: StackCodec,
    private val planRewards: (List<QuestReward>) -> Pair<List<StateChange>, List<ItemStack>>,
    private val hooks: OperationHooks,
    private val currency: CurrencyGateway,
    commitTimeout: Duration = 10.seconds,
) {
    /** The players with a running operation. */
    private val running: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /** Commits, claims and stores deliveries. */
    val deliveries = Deliveries(backend, codec, hooks, commitTimeout)

    /**
     * Runs [plan] for [player], taking from and giving to [inventory].
     *
     * @return [OperationOutcome.Busy] without doing anything when another operation of [player] is running
     */
    suspend fun run(player: UUID, inventory: EscrowInventory, plan: OperationPlan): OperationOutcome {
        if (!hooks.isAvailable()) return OperationOutcome.Unavailable
        if (!running.add(player)) return OperationOutcome.Busy
        try {
            return execute(player, inventory, plan)
        } finally {
            running.remove(player)
        }
    }

    /** Runs [plan] while the player's slot in [running] is held. */
    private suspend fun execute(player: UUID, inventory: EscrowInventory, plan: OperationPlan): OperationOutcome {
        val (rewardChanges, rewardItems) = planRewards(plan.rewards)
        val newDeliveries = rewardItems.map { NewDeliveryDto(codec.encode(it), plan.source) }
        val escrow = inventory.take(plan.take) ?: return OperationOutcome.MissingItems
        val operation = OperationDto(UUID.randomUUID(), player, plan.changes + rewardChanges, newDeliveries)

        val result = try {
            deliveries.commit(operation)
        } catch (e: CancellationException) {
            inventory.give(escrow).takeIf { it.isNotEmpty() }?.let { hooks.dropAtFeet(player, it) }
            throw e
        }

        return when (result) {
            is OperationResultDto.Applied -> succeed(player, inventory, plan, operation, result.deliveryIds)
            is OperationResultDto.AlreadyApplied -> succeed(player, inventory, plan, operation, result.deliveryIds)
            is OperationResultDto.Rejected -> {
                giveBack(player, inventory, escrow, plan.source)
                OperationOutcome.Rejected(result.reason)
            }
            null -> {
                giveBack(player, inventory, escrow, plan.source)
                OperationOutcome.Unavailable
            }
        }
    }

    /** Reports the stored operation, pays its currencies and hands out its deliveries. */
    private suspend fun succeed(
        player: UUID,
        inventory: EscrowInventory,
        plan: OperationPlan,
        operation: OperationDto,
        deliveryIds: List<UUID>
    ): OperationOutcome {
        hooks.onCommitted(player, operation.changes)
        RewardPlanner.currencies(plan.rewards).forEach { currency.grant(player, it.currencyId, it.amount) }
        deliveries.deliver(player, inventory, deliveryIds, plan.source)
        return OperationOutcome.Success
    }

    /** Gives the escrow back; what does not fit goes to the pickup box. */
    private suspend fun giveBack(player: UUID, inventory: EscrowInventory, escrow: List<ItemStack>, source: String) {
        val overflow = inventory.give(escrow)
        if (overflow.isNotEmpty() && deliveries.stash(player, overflow, source)) hooks.onLeftInPickupBox(player)
    }
}
