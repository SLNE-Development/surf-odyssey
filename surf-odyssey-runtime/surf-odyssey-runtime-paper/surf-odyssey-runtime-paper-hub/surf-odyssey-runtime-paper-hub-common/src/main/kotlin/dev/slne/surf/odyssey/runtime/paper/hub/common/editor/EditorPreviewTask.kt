package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import org.bukkit.Color
import org.bukkit.Particle
import kotlin.math.ceil
import kotlin.math.sqrt

/** Draws every open session's parts as particle lines to its owner, twice per second. */
object EditorPreviewTask {
    private const val STEP = 0.5
    private val INCLUDED = Particle.DustOptions(Color.LIME, 1f)
    private val EXCLUDED = Particle.DustOptions(Color.RED, 1f)

    /** Starts drawing for [hub]: included parts in green, excluded parts in red. */
    fun start(hub: HubPlugin) {
        hub.server.scheduler.runTaskTimer(hub, Runnable {
            for ((owner, session) in EditorSessions.all()) {
                val player = hub.server.getPlayer(owner) ?: continue
                for (part in session.parts) {
                    val dust = if (part.excluded) EXCLUDED else INCLUDED
                    for ((a, b) in ShapeOutline.edges(part)) {
                        val length = sqrt((b.first - a.first).let { it * it } + (b.second - a.second).let { it * it } + (b.third - a.third).let { it * it })
                        val steps = ceil(length / STEP).toInt().coerceAtLeast(1)
                        for (i in 0..steps) {
                            val t = i.toDouble() / steps
                            player.spawnParticle(Particle.DUST, a.first + (b.first - a.first) * t, a.second + (b.second - a.second) * t, a.third + (b.third - a.third) * t, 1, dust)
                        }
                    }
                }
            }
        }, 10L, 10L)
    }
}
