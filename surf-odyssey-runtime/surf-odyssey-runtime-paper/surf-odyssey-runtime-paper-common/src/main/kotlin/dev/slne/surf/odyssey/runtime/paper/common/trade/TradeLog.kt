package dev.slne.surf.odyssey.runtime.paper.common.trade

import dev.slne.surf.odyssey.core.common.trade.TradeBridge
import dev.slne.surf.odyssey.core.common.trade.TradeCancelType
import dev.slne.surf.odyssey.core.common.trade.TradeOutcome
import dev.slne.surf.odyssey.core.common.trade.TradeRecord
import dev.slne.surf.odyssey.core.common.trade.TradeRequestRecord
import dev.slne.surf.odyssey.core.common.trade.TradeRequestStatus
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.bukkit.inventory.ItemStack
import java.util.Base64
import java.util.logging.Level

private const val FLUSH_TIMEOUT_MILLIS = 5_000L

/**
 * Writes finished trades and resolved trade requests to the database in the background.
 *
 * A failed write is logged and never affects the trade itself.
 */
internal object TradeLog {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Stores the finished trade described by [snapshot]. */
    fun trade(snapshot: TradeSnapshot) {
        val record = TradeRecord(
            id = snapshot.id,
            firstUuid = snapshot.first,
            secondUuid = snapshot.second,
            outcome = if (snapshot.completed) TradeOutcome.COMPLETED else TradeOutcome.CANCELLED,
            cancelType = snapshot.cancelReason?.takeUnless { snapshot.completed }?.toCancelType(),
            cancelledBy = snapshot.cancelledBy.takeUnless { snapshot.completed },
            startedAtMillis = snapshot.startedAt,
            endedAtMillis = snapshot.endedAt,
            firstItems = snapshot.firstItems.map { it.encode() },
            secondItems = snapshot.secondItems.map { it.encode() }
        )

        save("trade ${snapshot.id}") { TradeBridge.saveTrade(record) }
    }

    /**
     * Stores that [request] ended with [status] at [now]. A request that had already run out is stored as
     * [TradeRequestStatus.EXPIRED] whatever [status] says.
     */
    fun request(request: TradeRequest, status: TradeRequestStatus, now: Long) {
        val record = TradeRequestRecord(
            id = request.id,
            senderUuid = request.sender,
            receiverUuid = request.receiver,
            status = if (request.isExpired(now)) TradeRequestStatus.EXPIRED else status,
            createdAtMillis = request.createdAt,
            resolvedAtMillis = now
        )

        save("trade request ${request.id}") { TradeBridge.saveRequest(record) }
    }

    /** Waits a few seconds for the writes that are still running, so a shutdown does not lose them. */
    suspend fun flush() {
        val pending = scope.coroutineContext[Job]?.children?.toList().orEmpty()
        withTimeoutOrNull(FLUSH_TIMEOUT_MILLIS) { pending.joinAll() }
        scope.cancel()
    }

    private fun save(description: String, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "Failed to save $description", e)
            }
        }
    }

    private fun TradeCancelReason.toCancelType() = when (this) {
        TradeCancelReason.CLOSED -> TradeCancelType.CLOSED
        TradeCancelReason.LEFT -> TradeCancelType.LEFT
        TradeCancelReason.SHUTDOWN -> TradeCancelType.SHUTDOWN
    }

    private fun ItemStack.encode(): String = Base64.getEncoder().encodeToString(serializeAsBytes())
}
