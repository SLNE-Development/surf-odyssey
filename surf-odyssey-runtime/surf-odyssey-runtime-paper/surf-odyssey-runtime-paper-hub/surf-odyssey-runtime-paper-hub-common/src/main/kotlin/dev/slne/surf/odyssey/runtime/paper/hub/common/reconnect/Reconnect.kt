package dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect

import dev.slne.surf.odyssey.core.common.reconnect.ReconnectEntry
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition

/** Turns logout positions into reconnect entries and back. */
object Reconnect {
    /**
     * Returns the reconnect entry of a player leaving the server [serverName] of [category] at [position] while the
     * server runs the map with hash [worldHash].
     */
    fun entry(category: String, serverName: String, position: WorldPosition, worldHash: String) = ReconnectEntry(
        category = category,
        serverName = serverName,
        x = position.x,
        y = position.y,
        z = position.z,
        yaw = position.yaw,
        pitch = position.pitch,
        worldHash = worldHash,
    )

    /**
     * Returns the logout position stored in [entry] if it belongs to this server's [category] and was recorded on the
     * same map, identified by [worldHash].
     *
     * @return the logout position, or `null` if there is no entry, it belongs to another category or the map changed
     */
    fun logoutPosition(entry: ReconnectEntry?, category: String, worldHash: String): WorldPosition? {
        if (entry == null || entry.category != category || entry.worldHash != worldHash) return null
        return WorldPosition(entry.x, entry.y, entry.z, entry.yaw, entry.pitch)
    }
}
