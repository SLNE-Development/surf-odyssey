package dev.slne.surf.odyssey.core.common.trade

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import kotlinx.serialization.Serializable

/** How a trade between two players ended. */
@Serializable
enum class TradeOutcome {
    /** Both players confirmed and the items were exchanged. */
    COMPLETED,

    /** The trade ended without an exchange and every player got their own items back. */
    CANCELLED
}

/** Why a cancelled trade was cancelled. */
@Serializable
enum class TradeCancelType {
    /** A player cancelled the trade or closed the trade window. */
    CLOSED,

    /** A player left the server during the trade. */
    LEFT,

    /** The server shut down during the trade. */
    SHUTDOWN
}

/** How a trade request ended. */
@Serializable
enum class TradeRequestStatus {
    /** The receiver accepted the request and a trade started. */
    ACCEPTED,

    /** The receiver declined the request. */
    DECLINED,

    /** The sender withdrew the request. */
    WITHDRAWN,

    /** Nobody answered before the request ran out. */
    EXPIRED,

    /** The request was dropped because a player left, started another trade or the server shut down. */
    CANCELLED
}

/**
 * A finished trade as stored in the database.
 *
 * @property id the unique id of the trade
 * @property firstUuid the player who sent the trade request
 * @property secondUuid the player who accepted the trade request
 * @property outcome how the trade ended
 * @property cancelType why the trade was cancelled, `null` for a completed trade
 * @property cancelledBy the player who cancelled the trade, `null` for a completed trade or a cancel without a player
 * @property startedAtMillis the epoch milliseconds at which the trade window opened
 * @property endedAtMillis the epoch milliseconds at which the trade completed or was cancelled
 * @property firstItems the items [firstUuid] offered, each as a Base64 encoded item stack
 * @property secondItems the items [secondUuid] offered, each as a Base64 encoded item stack
 */
@Serializable
data class TradeRecord(
    val id: SerializableUUID,
    val firstUuid: SerializableUUID,
    val secondUuid: SerializableUUID,
    val outcome: TradeOutcome,
    val cancelType: TradeCancelType?,
    val cancelledBy: SerializableUUID?,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val firstItems: List<String>,
    val secondItems: List<String>
)

/**
 * A resolved trade request as stored in the database.
 *
 * @property id the unique id of the request
 * @property senderUuid the player who sent the request
 * @property receiverUuid the player the request was sent to
 * @property status how the request ended
 * @property createdAtMillis the epoch milliseconds at which the request was sent
 * @property resolvedAtMillis the epoch milliseconds at which the request ended
 */
@Serializable
data class TradeRequestRecord(
    val id: SerializableUUID,
    val senderUuid: SerializableUUID,
    val receiverUuid: SerializableUUID,
    val status: TradeRequestStatus,
    val createdAtMillis: Long,
    val resolvedAtMillis: Long
)
