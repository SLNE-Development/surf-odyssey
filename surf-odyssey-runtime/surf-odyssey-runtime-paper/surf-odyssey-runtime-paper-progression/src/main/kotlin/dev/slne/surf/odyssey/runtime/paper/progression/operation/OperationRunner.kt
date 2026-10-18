package dev.slne.surf.odyssey.runtime.paper.progression.operation

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.slne.surf.odyssey.core.common.progression.DeliveryDto
import dev.slne.surf.odyssey.core.common.progression.OperationDto
import dev.slne.surf.odyssey.core.common.progression.OperationResultDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionBridge
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.runtime.paper.progression.ItemCodec
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.delivery.ClaimResult
import dev.slne.surf.odyssey.runtime.paper.progression.state.ProgressionStore
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Runs item operations and delivery claims for online players on the player's own thread.
 *
 * @param plugin the plugin that owns the coroutines and the log
 * @param store the cached progression the stored changes are applied to
 * @param currency pays currency rewards
 * @param bridge the progression service
 */
class OperationRunner(
    private val plugin: Plugin,
    private val store: ProgressionStore,
    currency: CurrencyGateway,
    bridge: ProgressionBridge = ProgressionBridge,
) {
    /** The players with a running operation or claim and how many are running, for dropping items at their feet. */
    private val players = ConcurrentHashMap<UUID, Pair<Player, Int>>()

    /** Connects the flow to this server. */
    private val hooks = object : OperationHooks {
        /** Returns [Progression.isAvailable]. */
        override fun isAvailable(): Boolean = Progression.isAvailable

        /** Marks the service unavailable and logs the failure once per outage. */
        override fun onServiceFailure(action: String, error: Throwable) {
            Progression.reportFailure(plugin.logger, action, error)
        }

        /** Applies the stored changes to the cached progression. */
        override fun onCommitted(player: UUID, changes: List<StateChange>) {
            store.apply(player, changes)
        }

        /** Tells the player that items wait in the pickup box. */
        override fun onLeftInPickupBox(player: UUID) {
            players[player]?.first?.takeIf { it.isOnline }?.let(ProgressionMessages::leftInPickupBox)
        }

        /** Drops the items at the player's last location and logs them. */
        override fun dropAtFeet(player: UUID, items: List<ItemStack>) {
            val holder = players[player]?.first
            if (holder == null) {
                plugin.logger.severe("Lost ${items.size} stacks of $player: ${items.joinToString { it.type.name }}")
                return
            }
            val listed = items.joinToString { "${it.amount}x ${it.type.name}" }
            plugin.logger.warning("Dropped ${items.size} stacks at the feet of $player: $listed")
            items.forEach { holder.world.dropItemNaturally(holder.location, it) }
        }

        /** Logs the lost delivery. */
        override fun onBrokenDelivery(player: UUID, deliveryId: UUID, error: Throwable) {
            plugin.logger.log(Level.SEVERE, "Could not decode the item of delivery $deliveryId of $player", error)
        }
    }

    /** The flow every operation runs through. */
    private val flow = OperationFlow(BridgeBackend(bridge), ItemCodec, { rewards, owner -> RewardPlanner.plan(rewards, owner) }, hooks, currency)

    /**
     * Runs one operation for [player] on the player's thread.
     *
     * @return the outcome; [OperationOutcome.Busy] without doing anything when another operation of the player is
     *         running, [OperationOutcome.Unavailable] when the service or the player's state is not available
     */
    suspend fun run(player: Player, plan: OperationPlan): OperationOutcome {
        if (!store.isAvailableFor(player.uniqueId)) return OperationOutcome.Unavailable
        return onPlayerThread(player) { flow.run(player.uniqueId, PlayerEscrowInventory(player), plan) }
    }

    /**
     * Claims one delivery from the pickup box of [player] and puts its item into the inventory, on the player's
     * thread. Nothing is claimed while an operation of the player runs or while the item does not fit.
     *
     * @param delivery the delivery to claim
     */
    suspend fun claim(player: Player, delivery: DeliveryDto): ClaimResult = onPlayerThread(player) {
        val inventory = PlayerEscrowInventory(player)
        flow.claim(player.uniqueId, inventory, delivery, inventory::fits)
    }

    /**
     * Puts [items] into the pickup box of [player] with a delivery operation of their own, which is committed once
     * more if the first attempt fails; works whether or not the player is online. When both attempts fail, the
     * items are dropped at the player's feet, or logged as lost when the player is not known here.
     *
     * @param player the owner of the pickup box
     * @param items the stacks to deliver
     * @param source the source stored with the deliveries
     * @return `true` if the items are in the pickup box
     */
    suspend fun stash(player: UUID, items: List<ItemStack>, source: String): Boolean =
        flow.deliveries.stash(player, items, source)

    /** Runs [block] on the thread of [player] while the player is registered in [players]. */
    private suspend fun <T> onPlayerThread(player: Player, block: suspend () -> T): T {
        players.compute(player.uniqueId) { _, entry -> player to ((entry?.second ?: 0) + 1) }
        try {
            return withContext(plugin.entityDispatcher(player)) { block() }
        } finally {
            players.computeIfPresent(player.uniqueId) { _, (holder, count) ->
                if (count > 1) holder to count - 1 else null
            }
        }
    }

    /**
     * Reports how an operation ended to [player]: missing items and an unreachable service are told, the other
     * outcomes are not.
     *
     * @param outcome the outcome of the operation
     */
    fun report(player: Player, outcome: OperationOutcome) {
        when (outcome) {
            OperationOutcome.MissingItems -> ProgressionMessages.missingItems(player)
            OperationOutcome.Unavailable -> ProgressionMessages.unavailable(player)
            OperationOutcome.Success, OperationOutcome.Busy, is OperationOutcome.Rejected -> Unit
        }
    }
}

/**
 * An [OperationBackend] that calls the progression service.
 *
 * @param bridge the progression service
 */
private class BridgeBackend(private val bridge: ProgressionBridge) : OperationBackend {
    /** Commits the operation at the progression service. */
    override suspend fun commit(operation: OperationDto): OperationResultDto = bridge.commitOperation(operation)

    /** Claims the delivery at the progression service. */
    override suspend fun claim(player: UUID, deliveryId: UUID): String? = bridge.claimDelivery(player, deliveryId)
}
