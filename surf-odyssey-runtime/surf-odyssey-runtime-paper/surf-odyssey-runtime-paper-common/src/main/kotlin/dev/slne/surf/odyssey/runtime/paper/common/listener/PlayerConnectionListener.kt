package dev.slne.surf.odyssey.runtime.paper.common.listener

import dev.slne.surf.odyssey.core.common.player.PlayerManager
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.logging.Level

/** Loads and caches the odyssey player before login and evicts it from the cache on quit. */
object PlayerConnectionListener : Listener {

    /**
     * Runs on the async login thread, so blocking until the player is loaded keeps the login waiting
     * instead of letting the player join without cached data.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAsyncPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return

        runCatching {
            runBlocking { PlayerManager.findOrCreatePlayer(event.uniqueId, cache = true) }
        }.onFailure {
            plugin.logger.log(Level.SEVERE, "Failed to load odyssey player ${event.uniqueId}", it)
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("Deine Spielerdaten konnten nicht geladen werden. Bitte versuche es erneut.")
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        PlayerManager.uncachePlayer(event.player.uniqueId)
    }
}
