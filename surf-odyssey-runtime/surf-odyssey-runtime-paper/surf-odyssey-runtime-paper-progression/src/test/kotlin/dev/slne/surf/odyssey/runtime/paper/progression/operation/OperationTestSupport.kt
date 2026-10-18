package dev.slne.surf.odyssey.runtime.paper.progression.operation

import dev.slne.surf.odyssey.core.common.progression.DeliveryDto
import dev.slne.surf.odyssey.core.common.progression.OperationDto
import dev.slne.surf.odyssey.core.common.progression.OperationResultDto
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.StackCodec
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Item references used by the operation tests. */
object TestItems {
    /** Plain ink sacs. */
    val ink: ItemRef = Material.INK_SAC.ref
}

/**
 * Creates stand-in item stacks that work without a running server and remembers which item and amount each one
 * stands for.
 */
object FakeStacks {
    /** The item and amount of every created stack. */
    private val contents = ConcurrentHashMap<ItemStack, Pair<ItemRef, Int>>()

    /** Creates a stand-in stack of [amount] times [ref]. */
    fun of(ref: ItemRef, amount: Int): ItemStack = mockk<ItemStack>().also { contents[it] = ref to amount }

    /** Returns the item and amount [stack] stands for. */
    fun contentOf(stack: ItemStack): Pair<ItemRef, Int> = checkNotNull(contents[stack]) { "not a fake stack" }

    /** Creates one stand-in stack holding the whole amount, as a stack factory for [RewardPlanner.plan]. */
    fun factory(ref: ItemRef, amount: Int): List<ItemStack> = listOf(of(ref, amount))
}

/** Encodes stand-in stacks as opaque keys. */
object FakeCodec : StackCodec {
    /** The stack behind every issued key. */
    private val stacks = ConcurrentHashMap<String, ItemStack>()

    /** Issues a new key for [stack]. */
    override fun encode(stack: ItemStack): String = UUID.randomUUID().toString().also { stacks[it] = stack }

    /** Returns the stack behind [encoded]. */
    override fun decode(encoded: String): ItemStack = checkNotNull(stacks[encoded]) { "unknown key" }
}

/**
 * A player inventory in memory.
 *
 * @param initial the amount of each item the inventory starts with
 * @param freeSlots how many more stacks fit; each given stack takes one slot
 */
class FakeInventory(initial: Map<ItemRef, Int>, private var freeSlots: Int = 36) : EscrowInventory {
    /** The amount of each item in the inventory. */
    private val counts = initial.toMutableMap()

    /** How many items [dropAll] threw away. */
    var dropped = 0
        private set

    /** Returns the amount of [ref] in the inventory. */
    override fun count(ref: ItemRef): Int = counts[ref] ?: 0

    /** Removes all [ingredients] and returns them as stacks, or removes nothing if one is short. */
    override fun take(ingredients: List<Ingredient>): List<ItemStack>? {
        if (ingredients.any { count(it.item) < it.amount }) return null
        return ingredients.map {
            counts[it.item] = count(it.item) - it.amount
            FakeStacks.of(it.item, it.amount)
        }
    }

    /** Adds each stack while slots are free and returns the stacks that did not fit. */
    override fun give(items: List<ItemStack>): List<ItemStack> = items.filter { stack ->
        if (freeSlots <= 0) return@filter true
        freeSlots--
        val (ref, amount) = FakeStacks.contentOf(stack)
        counts[ref] = count(ref) + amount
        false
    }

    /** Throws everything in the inventory away, as a player dropping all items would. */
    fun dropAll() {
        dropped += counts.values.sum()
        counts.clear()
    }
}

/**
 * Runs operations of one player through [OperationFlow] against an in-memory progression service.
 *
 * @param commit answers every commit, including the ones that put items into the pickup box
 * @param claimFails whether claiming a delivery throws
 * @param corruptFirstClaim whether the first claimed delivery hands out an item that cannot be decoded
 * @param commitTimeout how long a commit may take
 */
class TestRunner(
    private val commit: suspend (OperationDto) -> OperationResultDto,
    private val claimFails: Boolean = false,
    private var corruptFirstClaim: Boolean = false,
    commitTimeout: Duration = 10.seconds,
) {
    /** The player every operation runs for. */
    val player: UUID = UUID.randomUUID()

    /** The stored deliveries by id, with whether they were claimed. */
    private val deliveries = LinkedHashMap<UUID, Boolean>()

    /** The changes the cache received, one list per applied operation. */
    val appliedChanges = mutableListOf<List<StateChange>>()

    /** The stacks dropped at the player's feet. */
    val droppedAtFeet = mutableListOf<ItemStack>()

    /** How often the player was told that items are waiting in the pickup box. */
    var pickupNotices = 0
        private set

    /** The deliveries whose item could not be decoded. */
    val brokenDeliveries = mutableListOf<UUID>()

    /** How often a call to the service failed. */
    var serviceFailures = 0
        private set

    /** The currency grants, as currency id and amount. */
    val grants = mutableListOf<Pair<String, Double>>()

    /** The number of stored deliveries nobody claimed. */
    val unclaimedDeliveries: Int
        get() = deliveries.values.count { !it }

    /** Records each change of the service's state that the flow reports. */
    private val hooks = object : OperationHooks {
        override fun isAvailable(): Boolean = true
        override fun onServiceFailure(action: String, error: Throwable) {
            serviceFailures++
        }

        override fun onCommitted(player: UUID, changes: List<StateChange>) {
            appliedChanges += changes
        }

        override fun onLeftInPickupBox(player: UUID) {
            pickupNotices++
        }

        override fun dropAtFeet(player: UUID, items: List<ItemStack>) {
            droppedAtFeet += items
        }

        override fun onBrokenDelivery(player: UUID, deliveryId: UUID, error: Throwable) {
            brokenDeliveries += deliveryId
        }
    }

    /** Stores the deliveries of applied commits and hands each one out once. */
    private val backend = object : OperationBackend {
        override suspend fun commit(operation: OperationDto): OperationResultDto {
            val result = this@TestRunner.commit(operation)
            if (result is OperationResultDto.Applied) {
                result.deliveryIds.forEach { deliveries[it] = false }
                delivered.putAll(result.deliveryIds.zip(operation.deliveries.map { it.itemBase64 }))
            }
            return result
        }

        override suspend fun claim(player: UUID, deliveryId: UUID): String? {
            if (claimFails) error("claim failed")
            if (deliveries[deliveryId] != false) return null
            deliveries[deliveryId] = true
            if (corruptFirstClaim) {
                corruptFirstClaim = false
                return "corrupt"
            }
            return delivered[deliveryId]
        }
    }

    /** The encoded item of every stored delivery. */
    private val delivered = mutableMapOf<UUID, String>()

    /** The flow under test. */
    private val flow = OperationFlow(
        backend = backend,
        codec = FakeCodec,
        planRewards = { rewards, owner -> RewardPlanner.plan(rewards, owner, FakeStacks::factory) },
        hooks = hooks,
        currency = object : CurrencyGateway {
            override fun grant(player: UUID, currencyId: String, amount: Double) {
                grants += currencyId to amount
            }
        },
        commitTimeout = commitTimeout,
    )

    /** Claims [delivery] for [player] into [inventory], with [fits] deciding whether its item has room. */
    suspend fun claim(inventory: EscrowInventory, delivery: DeliveryDto, fits: (ItemStack) -> Boolean = { true }) =
        flow.claim(player, inventory, delivery, fits)

    /** Runs [plan] for [player] against [inventory]. */
    suspend fun run(inventory: EscrowInventory, plan: OperationPlan): OperationOutcome =
        flow.run(player, inventory, plan)
}
