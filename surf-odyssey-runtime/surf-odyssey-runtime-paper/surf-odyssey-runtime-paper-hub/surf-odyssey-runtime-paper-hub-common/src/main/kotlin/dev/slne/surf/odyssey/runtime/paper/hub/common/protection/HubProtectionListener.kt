package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.regeneration.RegenerationTicker
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.HarvestRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.PlayerRegionTracker
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent

/**
 * Applies the region rules to block breaks in the hub world, forwards block interactions to their
 * region and keeps track of the region each player stands in.
 */
object HubProtectionListener : Listener {
    private val hub get() = HubPlugin.instance

    /**
     * Allows, harvests or cancels a block break according to [BlockRules].
     *
     * An allowed break first calls the region's [Region.onBreak], which may still cancel it. A harvested block then turns into the region's placeholder and is scheduled
     * to regenerate.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.world != hub.hubWorld) return
        val point = BlockPoint(block.x, block.y, block.z)
        val region = hub.regionIndex.regionAt(point.x, point.y, point.z)
        val decision = BlockRules.decide(region, block.type, RegenerationTicker.queue.isPending(point))
        if (decision == BreakDecision.DENY) {
            event.isCancelled = true
            return
        }
        if (region == null) return
        region.onBreak(event)
        if (event.isCancelled || decision != BreakDecision.HARVEST || region !is HarvestRegion) return
        val original = block.blockData.clone()
        region.drops?.let { provider ->
            event.isDropItems = false
            provider(block).forEach { block.world.dropItemNaturally(block.location.toCenterLocation(), it) }
        }
        RegenerationTicker.schedule(point, original, region.regenerateAfter.inWholeMilliseconds)
        hub.server.scheduler.runTask(hub, Runnable { block.setType(region.placeholder, false) })
    }

    /** Forwards block interactions to the region they happen in. */
    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (block.world != hub.hubWorld) return
        hub.regionIndex.regionAt(block.x, block.y, block.z)?.onInteract(event)
    }

    /** Records the region a player joins in. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) = PlayerRegionTracker.update(event.player, regionAt(event.player.location))

    /** Updates the player's current region when they change block. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        if (!event.hasChangedBlock()) return
        PlayerRegionTracker.update(event.player, regionAt(event.to))
    }

    /** Updates the player's current region when they teleport. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) = PlayerRegionTracker.update(event.player, regionAt(event.to))

    /** Updates the player's current region when they respawn. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onRespawn(event: PlayerRespawnEvent) = PlayerRegionTracker.update(event.player, regionAt(event.respawnLocation))

    /** Forgets per-player state on quit. */
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        PlayerRegionTracker.remove(event.player)
    }

    /** Returns the region covering [location], or `null` outside the hub world or every region. */
    private fun regionAt(location: Location): Region? =
        if (location.world == hub.hubWorld) hub.regionIndex.regionAt(location.blockX, location.blockY, location.blockZ) else null
}
