package dev.slne.surf.odyssey.runtime.paper.common.trade

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal const val TRADE_REQUEST_TIMEOUT_MILLIS = 60_000L

internal const val TRADE_REQUEST_COOLDOWN_MILLIS = 5_000L

internal const val MAX_OUTGOING_TRADE_REQUESTS = 5

/** The outcome of sending a trade request. */
internal enum class TradeRequestResult {
    SENT,
    ALREADY_TRADING,
    PARTNER_BUSY,
    REQUESTS_DISABLED,
    COOLDOWN,
    ALREADY_PENDING,
    LIMIT_REACHED
}

/**
 * A pending trade request.
 *
 * @property id the unique id of the request
 * @property sender the player who sent the request
 * @property senderName the name of [sender] when the request was sent
 * @property receiver the player the request was sent to
 * @property receiverName the name of [receiver] when the request was sent
 * @property createdAt the epoch milliseconds at which the request was sent
 * @property expiresAt the epoch milliseconds from which the request is no longer valid
 */
internal class TradeRequest(
    val sender: UUID,
    val senderName: String,
    val receiver: UUID,
    val receiverName: String,
    val createdAt: Long,
    val expiresAt: Long,
    val id: UUID = UUID.randomUUID()
) {
    /** Whether the request ran out at [now]. */
    fun isExpired(now: Long) = expiresAt <= now
}

/**
 * Keeps the pending trade requests and enforces the send cooldown and the limit of open outgoing requests.
 *
 * Every `take` function removes the requests it returns, expired ones included, so the caller can record how each of
 * them ended.
 */
internal class TradeRequestBook(
    private val timeoutMillis: Long = TRADE_REQUEST_TIMEOUT_MILLIS,
    private val cooldownMillis: Long = TRADE_REQUEST_COOLDOWN_MILLIS,
    private val maxOutgoing: Int = MAX_OUTGOING_TRADE_REQUESTS
) {
    private data class RequestKey(val sender: UUID, val receiver: UUID)

    private val requests = ConcurrentHashMap<RequestKey, TradeRequest>()
    private val lastSentAt = ConcurrentHashMap<UUID, Long>()
    private val lock = Any()

    /**
     * Adds a request from [sender] to [receiver] unless the sender is on cooldown, already has a request to the
     * receiver or has [maxOutgoing] open requests. Expired requests do not count towards the limit.
     *
     * @return [TradeRequestResult.SENT] if the request was added, otherwise the reason it was refused
     */
    fun add(sender: UUID, senderName: String, receiver: UUID, receiverName: String, now: Long): TradeRequestResult =
        synchronized(lock) {
            if (cooldownRemaining(sender, now) > 0) return TradeRequestResult.COOLDOWN

            val key = RequestKey(sender, receiver)
            val existing = requests[key]
            if (existing != null && !existing.isExpired(now)) return TradeRequestResult.ALREADY_PENDING

            val open = requests.values.count { it.sender == sender && !it.isExpired(now) && it !== existing }
            if (open >= maxOutgoing) return TradeRequestResult.LIMIT_REACHED

            requests[key] = TradeRequest(sender, senderName, receiver, receiverName, now, now + timeoutMillis)
            lastSentAt[sender] = now
            TradeRequestResult.SENT
        }

    /** The milliseconds [sender] still has to wait before sending the next request, `0` if they can send now. */
    fun cooldownRemaining(sender: UUID, now: Long): Long {
        val last = lastSentAt[sender] ?: return 0L
        return (last + cooldownMillis - now).coerceAtLeast(0L)
    }

    /** Removes and returns the request from [sender] to [receiver], or `null` if there is none. */
    fun take(sender: UUID, receiver: UUID): TradeRequest? = requests.remove(RequestKey(sender, receiver))

    /** Removes and returns every request [sender] has sent. */
    fun takeFrom(sender: UUID): List<TradeRequest> = takeWhere { it.sender == sender }

    /** Removes and returns every request that [player] sent or received. */
    fun takeInvolving(player: UUID): List<TradeRequest> = takeWhere { it.sender == player || it.receiver == player }

    /** Removes and returns every request that ran out at [now]. */
    fun takeExpired(now: Long): List<TradeRequest> = takeWhere { it.isExpired(now) }

    /** Removes and returns every request. */
    fun takeAll(): List<TradeRequest> = takeWhere { true }

    /** The valid requests [player] sent or received at [now], received ones first, each group oldest first. */
    fun involving(player: UUID, now: Long): List<TradeRequest> = requests.values
        .filter { !it.isExpired(now) && (it.sender == player || it.receiver == player) }
        .sortedWith(compareBy({ it.receiver != player }, { it.createdAt }))

    /** Forgets the send cooldown of [player]. */
    fun forgetCooldown(player: UUID) {
        lastSentAt.remove(player)
    }

    private fun takeWhere(predicate: (TradeRequest) -> Boolean): List<TradeRequest> {
        val taken = mutableListOf<TradeRequest>()

        requests.entries.removeIf { (_, request) ->
            predicate(request).also { if (it) taken.add(request) }
        }

        return taken
    }
}
