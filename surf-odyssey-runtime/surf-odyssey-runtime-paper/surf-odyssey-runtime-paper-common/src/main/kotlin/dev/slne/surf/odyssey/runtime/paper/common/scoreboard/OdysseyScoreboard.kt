@file:Suppress("DEPRECATION")

package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

import dev.slne.surf.api.paper.scoreboard.SurfAutoUpdatableScoreboard
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardBuilder
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

/** Owns the scoreboard that is shown to the players who did not switch it off. */
internal object OdysseyScoreboard {
    @Volatile
    private var scoreboard: SurfAutoUpdatableScoreboard? = null

    private val providerFailureLogged = AtomicBoolean(false)

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

    /** Closes the scoreboard for all players. Calls to [show] and [hide] afterwards do nothing. */
    fun stop() {
        val current = scoreboard ?: return
        scoreboard = null
        current.disable()
    }

    /** Draws the viewer lines and the clock lines for [viewer]. */
    private fun componentOf(viewer: Player): SidebarComponent {
        val lines = ScoreboardLayout.viewerLines(snapshotOf(viewer)) +
            ScoreboardLayout.clockLines(ZonedDateTime.now(ScoreboardLayout.ZONE))
        return SidebarComponent.builder().apply { lines.forEach { addStaticLine(it) } }.build()
    }

    /** Collects what the scoreboard shows [viewer] right now; title and quest are left out if the provider fails. */
    private fun snapshotOf(viewer: Player): ScoreboardSnapshot {
        val provider = ScoreboardProviders.current
        return ScoreboardSnapshot(
            playerName = viewer.name,
            title = provider?.let { attempt { it.titleOf(viewer) } },
            quest = provider?.let { attempt { it.questOf(viewer) } },
            ping = viewer.ping,
            online = Bukkit.getOnlinePlayers().size,
        )
    }

    /** Returns the result of [block], or `null` if it throws; the first failure is logged. */
    private fun <T> attempt(block: () -> T?): T? = try {
        block()
    } catch (e: Exception) {
        if (providerFailureLogged.compareAndSet(false, true)) {
            plugin.logger.log(Level.SEVERE, "The scoreboard provider failed; title and quest are hidden for now", e)
        }
        null
    }
}
