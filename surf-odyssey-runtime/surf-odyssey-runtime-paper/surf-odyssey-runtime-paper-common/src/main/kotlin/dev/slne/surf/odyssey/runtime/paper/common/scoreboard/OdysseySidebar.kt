package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

import dev.slne.surf.api.paper.sidebar.SurfAutoUpdatingSidebar
import dev.slne.surf.api.paper.sidebar.autoUpdatingSidebar
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import kotlin.time.Duration.Companion.seconds

/** Owns the sidebar that shows the scoreboard to the players who did not switch it off. */
internal object OdysseySidebar {
    private var sidebar: SurfAutoUpdatingSidebar? = null

    /** Creates the sidebar, owned by [plugin], that refreshes every 2 seconds. Does nothing if it exists. */
    fun start(plugin: Plugin) {
        if (sidebar != null) return
        sidebar = autoUpdatingSidebar(plugin) {
            updateInterval = 2.seconds
            title(ScoreboardLayout.title())
            lines { viewer -> ScoreboardLayout.lines(snapshotOf(viewer)) }
        }
    }

    /** Shows the scoreboard to [player]. */
    fun show(player: Player) {
        sidebar?.addViewer(player)
    }

    /** Removes the scoreboard from [player]. */
    fun hide(player: Player) {
        sidebar?.removeViewer(player)
    }

    /** Closes the sidebar for all players. */
    fun stop() {
        sidebar?.close()
        sidebar = null
    }

    /** Collects what the scoreboard shows [viewer] right now. */
    private fun snapshotOf(viewer: Player): ScoreboardSnapshot {
        val provider = ScoreboardProviders.current
        return ScoreboardSnapshot(
            playerName = viewer.name,
            title = provider?.titleOf(viewer),
            quest = provider?.questOf(viewer),
            ping = viewer.ping,
            online = Bukkit.getOnlinePlayers().size,
        )
    }
}
