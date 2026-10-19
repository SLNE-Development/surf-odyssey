@file:Suppress("DEPRECATION")

package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

import dev.slne.surf.api.paper.scoreboard.SurfAutoUpdatableScoreboard
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardBuilder
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.ZonedDateTime

/** Owns the scoreboard that is shown to the players who did not switch it off. */
internal object OdysseyScoreboard {
    @Volatile
    private var scoreboard: SurfAutoUpdatableScoreboard? = null

    /** Creates and enables the scoreboard: the animated banner, then the lines of each viewer. Does nothing if it exists. */
    fun start() {
        if (scoreboard != null) return
        scoreboard = SurfScoreboardBuilder.builder(Component.empty())
            .addGradientLine(ScoreboardLayout.banner(), ScoreboardLayout.GRADIENT_START, ScoreboardLayout.GRADIENT_END)
            .addViewerComponent { viewer -> componentOf(viewer) }
            .buildAutoUpdatable()
            .also { it.enable() }
    }

    /** Shows the scoreboard to [player]. */
    fun show(player: Player) {
        scoreboard?.addViewer(player)
    }

    /** Removes the scoreboard from [player]. */
    fun hide(player: Player) {
        scoreboard?.removeViewer(player)
    }

    /** Closes the scoreboard for all players. */
    fun stop() {
        scoreboard?.disable()
        scoreboard = null
    }

    /** Draws the viewer lines and the clock lines for [viewer]. */
    private fun componentOf(viewer: Player): SidebarComponent {
        val lines = ScoreboardLayout.viewerLines(snapshotOf(viewer)) +
            ScoreboardLayout.clockLines(ZonedDateTime.now(ScoreboardLayout.ZONE))
        return SidebarComponent.builder().apply { lines.forEach { addStaticLine(it) } }.build()
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
