package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.progression.DoubleDrop
import dev.slne.surf.odyssey.runtime.paper.hub.common.progression.RegionProgress
import dev.slne.surf.odyssey.runtime.paper.progression.quest.ProgressEvent
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ToolBonuses
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import kotlin.random.Random

/**
 * Replaces the catch of a hook inside a fishing region with a fish from the region's table and cancels
 * every catch in the hub world outside fishing regions. The region and the water depth are taken at the
 * hook block if it is water, otherwise at the block below it.
 *
 * A catch from a region's table counts for the player's quests and for the rod in the main hand. A rod with a bite
 * speed bonus shortens the wait of its hook when it is cast in the hub world, and a rod with a double drop bonus may
 * put a second stack of the catch into the inventory of the player.
 */
object FishingListener : Listener {
    private const val MAX_DEPTH_SCAN = 16

    /** Shortens the wait of a hook cast in the hub world by the bite speed bonus of the rod that cast it. */
    @EventHandler(ignoreCancelled = true)
    fun onCast(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.FISHING) return
        if (event.hook.world != HubPlugin.instance.hubWorld) return
        val rod = event.hand?.let { event.player.inventory.getItem(it) } ?: event.player.inventory.itemInMainHand
        BiteSpeed.apply(event.hook, ToolBonuses.biteSpeedPercent(rod))
    }

    /**
     * Rolls the region's table when a fish is caught and reports the catch; outside every fishing region or
     * with no fitting fish the catch is cancelled. When the double drop bonus of the rod doubles the catch, a second
     * stack goes into the inventory of the player, or at the feet of the player if it does not fit.
     */
    @EventHandler(ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
        val hub = HubPlugin.instance
        val hook = event.hook.location
        if (hook.world != hub.hubWorld) return
        val isWaterAt = { offset: Int -> hook.block.getRelative(0, offset, 0).type == Material.WATER }
        val spot = hook.block.getRelative(0, WaterDepth.startOffset(isWaterAt), 0)
        val region = hub.regionIndex.regionAt(spot.x, spot.y, spot.z) as? FishingRegion
        if (region == null) {
            event.isCancelled = true
            return
        }
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
        val item = fish.item()
        caught.itemStack = item
        val player = event.player
        val rod = event.hand?.let { player.inventory.getItem(it) } ?: player.inventory.itemInMainHand
        if (DoubleDrop.roll(ToolBonuses.doubleDropChance(rod)) > 1) {
            player.inventory.addItem(item.clone()).values.forEach { player.world.dropItemNaturally(player.location, it) }
        }
        val name = PlainTextComponentSerializer.plainText().serialize(item.effectiveName())
        RegionProgress.fished(event.player, ProgressEvent.Fished(fish.id, item.type, region.id), fish.weight, name)
    }
}
