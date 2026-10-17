package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import org.bukkit.Color
import org.bukkit.Particle

/**
 * Draws every open session's parts as particle lines to its owner, twice per second. Owners outside
 * the hub world see nothing; edges farther than [MAX_DISTANCE] blocks from the owner are skipped and
 * each edge gets at most [Preview.MAX_STEPS] particles.
 */
object EditorPreviewTask {
    /** Farthest distance from the owner, in blocks, at which an edge is still drawn. */
    const val MAX_DISTANCE = 64.0
    private val INCLUDED = Particle.DustOptions(Color.LIME, 1f)
    private val EXCLUDED = Particle.DustOptions(Color.RED, 1f)

    /** Starts drawing for [hub]: included parts in green, excluded parts in red. */
    fun start(hub: HubPlugin) {
        hub.server.scheduler.runTaskTimer(hub, Runnable {
            for ((owner, session) in EditorSessions.all()) {
                val player = hub.server.getPlayer(owner) ?: continue
                if (player.world != hub.hubWorld) continue
                val position = player.location.let { Vec(it.x, it.y, it.z) }
                for (part in session.parts) {
                    val dust = if (part.excluded) EXCLUDED else INCLUDED
                    for ((a, b) in ShapeOutline.edges(part)) {
                        if (Preview.distance(position, a, b) > MAX_DISTANCE) continue
                        val steps = Preview.steps(Preview.length(a, b))
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
