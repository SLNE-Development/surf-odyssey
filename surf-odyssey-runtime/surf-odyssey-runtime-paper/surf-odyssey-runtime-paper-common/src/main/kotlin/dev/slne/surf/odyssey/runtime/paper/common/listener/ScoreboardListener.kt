package dev.slne.surf.odyssey.runtime.paper.common.listener

import dev.slne.surf.odyssey.runtime.paper.common.plugin
import dev.slne.surf.odyssey.runtime.paper.common.scoreboard.OdysseySidebar
import dev.slne.surf.odyssey.runtime.paper.common.scoreboard.scoreboardPreferences
import kotlinx.coroutines.runBlocking
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.logging.Level

/** Loads the scoreboard setting before login, shows the scoreboard on join and forgets the setting on quit. */
internal object ScoreboardListener : Listener {
    /** Loads the setting of the joining player; waits at most the load timeout and keeps the scoreboard on if it fails. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAsyncPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return

        runCatching { runBlocking { scoreboardPreferences.load(event.uniqueId) } }
            .onFailure { plugin.logger.log(Level.SEVERE, "Failed to load the scoreboard setting of ${event.uniqueId}", it) }
    }

    /** Shows the scoreboard to a joining player who did not switch it off. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (scoreboardPreferences.isEnabled(event.player.uniqueId)) OdysseySidebar.show(event.player)
    }

    /** Forgets the setting of a leaving player; the sidebar removes the viewer itself. */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        scoreboardPreferences.evict(event.player.uniqueId)
    }
}
