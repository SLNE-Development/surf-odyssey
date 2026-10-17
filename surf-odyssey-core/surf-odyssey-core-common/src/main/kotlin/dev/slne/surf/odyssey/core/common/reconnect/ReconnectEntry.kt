package dev.slne.surf.odyssey.core.common.reconnect

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Where a player was when they last left a hub server.
 *
 * The proxy uses it to send a returning player back to that server, and the hub server uses it to place the player
 * at their logout position if the map has not changed since.
 *
 * @property category the server category of the hub the player left
 * @property serverName the name of the hub server the player left
 * @property x the x coordinate of the logout position
 * @property y the y coordinate of the logout position
 * @property z the z coordinate of the logout position
 * @property yaw the horizontal view angle at logout in degrees
 * @property pitch the vertical view angle at logout in degrees
 * @property worldHash the hash of the map the hub server ran when the player left, or `null` if the server had none
 */
@Serializable
data class ReconnectEntry(
    val category: String,
    val serverName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val worldHash: String?,
) {
    companion object {
        /** Prefix of every server category that belongs to a hub world. */
        const val HUB_CATEGORY_PREFIX = "odyssey-hub-"

        /** Returns the Redis key under which the entry of [player] is stored without expiry. */
        fun redisKey(player: UUID): String = "odyssey:hub-reconnect:$player"
    }
}
