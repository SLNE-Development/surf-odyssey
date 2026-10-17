package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import kotlin.random.Random

/** Replaces the catch of a hook inside a fishing region with a fish from the region's table. */
object FishingListener : Listener {
    private const val MAX_DEPTH_SCAN = 16

    /** Rolls the region's table when a fish is caught; with no fitting fish the catch is cancelled. */
    @EventHandler(ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
        val hub = HubPlugin.instance
        val hook = event.hook.location
        if (hook.world != hub.hubWorld) return
        val region = hub.regionIndex.regionAt(hook.blockX, hook.blockY, hook.blockZ) as? FishingRegion ?: return
        val caught = event.caught as? Item ?: return
        val world = hook.world
        val context = FishContext(
            raining = world.hasStorm(),
            thundering = world.isThundering,
            night = world.time % 24_000 in 13_000..23_000,
            biome = hook.block.biome.key().asString(),
            waterDepth = depthBelow(hook.block),
        )
        val fish = region.table.roll(context, Random.Default)
        if (fish == null) {
            event.isCancelled = true
            return
        }
        caught.itemStack = fish.item()
    }

    /** Counts the water blocks under the hook in [start] as [WaterDepth.count] does, up to a fixed limit. */
    private fun depthBelow(start: Block): Int =
        WaterDepth.count({ start.getRelative(0, it, 0).type == Material.WATER }, MAX_DEPTH_SCAN)
}
