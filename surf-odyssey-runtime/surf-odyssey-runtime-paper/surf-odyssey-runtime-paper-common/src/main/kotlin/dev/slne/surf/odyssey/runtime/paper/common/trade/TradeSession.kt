package dev.slne.surf.odyssey.runtime.paper.common.trade

import me.devnatan.inventoryframework.context.RenderContext
import org.bukkit.inventory.ItemStack
import java.util.UUID

internal const val TRADE_OFFER_SLOTS = 16

internal const val TRADE_COUNTDOWN_MILLIS = 20_000L

internal enum class TradePhase {
    NEGOTIATING,

    COUNTDOWN,

    COMPLETED,

    CANCELLED;

    val isTerminal get() = this == COMPLETED || this == CANCELLED
}

internal enum class TradeCancelReason {
    CLOSED,

    LEFT,

    SHUTDOWN
}

internal class TradeSide(val uuid: UUID, val name: String) {
    val offer: Array<ItemStack?> = arrayOfNulls(TRADE_OFFER_SLOTS)
    var ready = false
    var settled = false

    @Volatile
    var context: RenderContext? = null
}

internal class TradeSettlement(
    val items: List<ItemStack>,
    val completed: Boolean,
    val cancelReason: TradeCancelReason?,
    val cancelledBy: UUID?
)

/**
 * What a finished trade looked like, taken once per session for the trade history.
 *
 * @property first the player who sent the trade request
 * @property second the player who accepted the trade request
 */
internal class TradeSnapshot(
    val id: UUID,
    val first: UUID,
    val second: UUID,
    val completed: Boolean,
    val cancelReason: TradeCancelReason?,
    val cancelledBy: UUID?,
    val startedAt: Long,
    val endedAt: Long,
    val firstItems: List<ItemStack>,
    val secondItems: List<ItemStack>
)

internal class TradeSession(val first: TradeSide, val second: TradeSide) {
    private val lock = Any()

    val id: UUID = UUID.randomUUID()
    val startedAt = System.currentTimeMillis()

    private var phaseValue = TradePhase.NEGOTIATING
    private var countdownEndsAtValue = 0L
    private var cancelReasonValue: TradeCancelReason? = null
    private var cancelledByValue: UUID? = null
    private var endedAtValue = 0L
    private var snapshotTaken = false

    val phase get() = synchronized(lock) { phaseValue }

    val countdownEndsAt get() = synchronized(lock) { countdownEndsAtValue }

    fun sideOf(uuid: UUID): TradeSide? = when (uuid) {
        first.uuid -> first
        second.uuid -> second
        else -> null
    }

    fun partnerOf(uuid: UUID): TradeSide = if (uuid == first.uuid) second else first

    fun offerItem(uuid: UUID, index: Int): ItemStack? = synchronized(lock) {
        sideOf(uuid)?.offer?.getOrNull(index)?.clone()
    }

    fun offerItems(uuid: UUID): List<ItemStack> = synchronized(lock) {
        sideOf(uuid)?.offer?.mapNotNull { it?.clone() }.orEmpty()
    }

    fun isReady(uuid: UUID) = synchronized(lock) { sideOf(uuid)?.ready == true }

    fun addItem(uuid: UUID, stack: ItemStack): Boolean = synchronized(lock) {
        val side = sideOf(uuid) ?: return false
        if (phaseValue.isTerminal || stack.isEmpty) return false

        val working = side.offer.map { it?.clone() }.toTypedArray()
        var remaining = stack.amount

        for (slot in working) {
            if (remaining <= 0) break
            if (slot == null || !slot.isSimilar(stack)) continue
            val moved = minOf(remaining, slot.maxStackSize - slot.amount)
            slot.amount += moved
            remaining -= moved
        }

        while (remaining > 0) {
            val free = working.indexOfFirst { it == null }
            if (free < 0) return false
            val moved = minOf(remaining, stack.maxStackSize)
            working[free] = stack.clone().also { it.amount = moved }
            remaining -= moved
        }

        working.copyInto(side.offer)
        resetReadiness()
        true
    }

    fun removeItem(uuid: UUID, index: Int): ItemStack? = synchronized(lock) {
        val side = sideOf(uuid) ?: return null
        if (phaseValue.isTerminal || index !in side.offer.indices) return null

        val item = side.offer[index] ?: return null
        side.offer[index] = null
        resetReadiness()
        item
    }

    fun restoreItem(uuid: UUID, index: Int, stack: ItemStack): Boolean = synchronized(lock) {
        val side = sideOf(uuid) ?: return false
        if (phaseValue.isTerminal) return false

        if (side.offer.getOrNull(index) == null) {
            side.offer[index] = stack
            resetReadiness()
            true
        } else {
            addItem(uuid, stack)
        }
    }

    fun setReady(uuid: UUID, ready: Boolean, now: Long): Boolean = synchronized(lock) {
        val side = sideOf(uuid) ?: return false
        if (phaseValue.isTerminal) return false

        if (ready && first.offer.all { it == null } && second.offer.all { it == null }) return false

        side.ready = ready

        if (first.ready && second.ready) {
            phaseValue = TradePhase.COUNTDOWN
            countdownEndsAtValue = now + TRADE_COUNTDOWN_MILLIS
        } else {
            phaseValue = TradePhase.NEGOTIATING
        }
        true
    }

    fun tryComplete(now: Long): Boolean = synchronized(lock) {
        if (phaseValue != TradePhase.COUNTDOWN || now < countdownEndsAtValue) return false
        if (!first.ready || !second.ready) return false

        phaseValue = TradePhase.COMPLETED
        endedAtValue = now
        true
    }

    fun cancel(by: UUID?, reason: TradeCancelReason): Boolean = synchronized(lock) {
        if (phaseValue.isTerminal) return false

        phaseValue = TradePhase.CANCELLED
        cancelReasonValue = reason
        cancelledByValue = by
        endedAtValue = System.currentTimeMillis()
        true
    }

    /**
     * Takes the snapshot of the finished trade for the trade history.
     *
     * @return the snapshot the first time it is called after the trade ended, `null` before the trade ended and on
     *         every later call
     */
    fun takeSnapshot(): TradeSnapshot? = synchronized(lock) {
        if (!phaseValue.isTerminal || snapshotTaken) return null

        snapshotTaken = true
        TradeSnapshot(
            id = id,
            first = first.uuid,
            second = second.uuid,
            completed = phaseValue == TradePhase.COMPLETED,
            cancelReason = cancelReasonValue,
            cancelledBy = cancelledByValue,
            startedAt = startedAt,
            endedAt = endedAtValue,
            firstItems = first.offer.mapNotNull { it?.clone() },
            secondItems = second.offer.mapNotNull { it?.clone() }
        )
    }

    fun settle(uuid: UUID): TradeSettlement? = synchronized(lock) {
        val side = sideOf(uuid) ?: return null
        if (!phaseValue.isTerminal || side.settled) return null

        side.settled = true
        val completed = phaseValue == TradePhase.COMPLETED
        val source = if (completed) partnerOf(uuid) else side

        TradeSettlement(
            items = source.offer.mapNotNull { it?.clone() },
            completed = completed,
            cancelReason = cancelReasonValue,
            cancelledBy = cancelledByValue
        )
    }

    private fun resetReadiness() {
        first.ready = false
        second.ready = false
        phaseValue = TradePhase.NEGOTIATING
    }
}
