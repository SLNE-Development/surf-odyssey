package dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/** Records where players leave this hub server. */
object ReconnectListener : Listener {
    /** Stores the leaving player's server, position and map hash. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) = ReconnectService.record(event.player)
}
