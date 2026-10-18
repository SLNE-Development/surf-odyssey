package dev.slne.surf.odyssey.runtime.paper.hub.common.region

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.regeneration.RegenerationTicker
import org.bukkit.scheduler.BukkitTask

/** Runs regeneration and the regions' tick hooks once per second on the main thread. */
object RegionTicker {
    private var task: BukkitTask? = null

    /** Starts ticking for [hub]. */
    fun start(hub: HubPlugin) {
        task = hub.server.scheduler.runTaskTimer(hub, Runnable {
            RegenerationTicker.tick(hub.hubWorld)
            hub.regionIndex.regions.forEach { it.onTick() }
        }, 20L, 20L)
    }

    /** Stops ticking. */
    fun stop() {
        task?.cancel()
        task = null
    }
}
