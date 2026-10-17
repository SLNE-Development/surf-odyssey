package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import kotlin.random.Random

/**
 * Replaces the catch of a hook inside a fishing region with a fish from the region's table. The
 * region and the water depth are taken at the hook block if it is water, otherwise at the block below it.
 */
object FishingListener : Listener {
    private const val MAX_DEPTH_SCAN = 16

    /** Rolls the region's table when a fish is caught; with no fitting fish the catch is cancelled. */
    @EventHandler(ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
        val hub = HubPlugin.instance
        val hook = event.hook.location
        if (hook.world != hub.hubWorld) return
        val isWaterAt = { offset: Int -> hook.block.getRelative(0, offset, 0).type == Material.WATER }
        val spot = hook.block.getRelative(0, WaterDepth.startOffset(isWaterAt), 0)
        val region = hub.regionIndex.regionAt(spot.x, spot.y, spot.z) as? FishingRegion ?: return
        val caught = event.caught as? Item ?: return
        val world = hook.world
        val context = FishContext(
            raining = world.hasStorm(),
            thundering = world.isThundering,
            night = world.time % 24_000 in 13_000..23_000,
            biome = spot.biome.key().asString(),
            waterDepth = WaterDepth.count(isWaterAt, MAX_DEPTH_SCAN),
        )
        val fish = region.table.roll(context, Random.Default)
        if (fish == null) {
            event.isCancelled = true
            return
        }
        caught.itemStack = fish.item()
    }
}
