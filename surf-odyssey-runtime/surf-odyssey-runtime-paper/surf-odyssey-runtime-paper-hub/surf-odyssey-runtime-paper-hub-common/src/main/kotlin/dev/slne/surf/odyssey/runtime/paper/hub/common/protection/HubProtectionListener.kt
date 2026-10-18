package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.editor.EditorSessions
import dev.slne.surf.odyssey.runtime.paper.hub.common.progression.DoubleDrop
import dev.slne.surf.odyssey.runtime.paper.hub.common.progression.PendingDoubleDrops
import dev.slne.surf.odyssey.runtime.paper.hub.common.progression.RegionProgress
import dev.slne.surf.odyssey.runtime.paper.progression.quest.ProgressEvent
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ToolBonuses
import dev.slne.surf.odyssey.runtime.paper.hub.common.regeneration.RegenerationTicker
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.HarvestRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.PlayerRegionTracker
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import com.destroystokyo.paper.event.block.BlockDestroyEvent
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent

/**
 * Applies the region rules to block breaks in the hub world and keeps track of the region each
 * player stands in.
 */
object HubProtectionListener : Listener {
    private val hub get() = HubPlugin.instance

    /** The harvested blocks without region drops whose vanilla drops drop twice. */
    private val doubledBlocks = PendingDoubleDrops()

    /**
     * Cancels a block break that [BlockRules] deny. An allowed break calls the region's
     * [Region.onBreak], which may still cancel it.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val (region, decision) = decide(event) ?: return
        if (decision == BreakDecision.DENY) {
            event.isCancelled = true
            return
        }
        region?.onBreak(event)
    }

    /**
     * Harvests a block whose break went through: replaces its drops if the region provides them,
     * schedules it to regenerate and, on the next tick, turns it into the region's placeholder if
     * it is still waiting to regenerate.
     *
     * The harvest counts for the player's quests and for the pickaxe (mines) or axe (forests) in the
     * main hand. With the double drop bonus of that tool the drops may drop twice: the region's drops
     * right away, the vanilla drops of a region without drops when they are spawned.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBreakHarvest(event: BlockBreakEvent) {
        val (region, decision) = decide(event) ?: return
        if (decision != BreakDecision.HARVEST || region !is HarvestRegion) return
        val block = event.block
        val point = BlockPoint(block.x, block.y, block.z)
        if (!RegenerationTicker.schedule(point, block.blockData.clone(), region.regenerateAfter.inWholeMilliseconds)) return
        val material = block.type
        val times = DoubleDrop.roll(ToolBonuses.doubleDropChance(event.player.inventory.itemInMainHand))
        val provider = region.drops
        if (provider != null) {
            event.isDropItems = false
            provider(block).forEach { drop ->
                repeat(times) { block.world.dropItemNaturally(block.location.toCenterLocation(), drop.clone()) }
            }
        } else if (times > 1) {
            doubledBlocks.mark(point)
        }
        RegionProgress.harvested(event.player, region, ProgressEvent.Harvested(material, region.id))
        hub.server.scheduler.runTask(hub, Runnable {
            doubledBlocks.forget(point)
            if (RegenerationTicker.queue.isPending(point)) block.setType(region.placeholder, false)
        })
    }

    /**
     * Drops the vanilla drops of a harvested block once more when the double drop bonus of the harvesting tool
     * doubled them.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onHarvestDrops(event: BlockDropItemEvent) {
        val block = event.block
        if (block.world != hub.hubWorld) return
        if (!doubledBlocks.take(BlockPoint(block.x, block.y, block.z))) return
        event.items.forEach { item -> block.world.dropItemNaturally(item.location, item.itemStack.clone()) }
    }

    /**
     * Cancels physics destroying a block in the hub world, e.g. a torch, ladder, cocoa pod, flower or
     * carpet losing the block it is attached to, unless [BlockRules.allowsDestroy] allow it. A cancelled
     * block stays in place and drops nothing.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onDestroy(event: BlockDestroyEvent) {
        val block = event.block
        if (block.world != hub.hubWorld) return
        val region = hub.regionIndex.regionAt(block.x, block.y, block.z)
        val pending = RegenerationTicker.queue.isPending(BlockPoint(block.x, block.y, block.z))
        if (!BlockRules.allowsDestroy(region, block.type, pending)) event.isCancelled = true
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
        EditorSessions.close(event.player.uniqueId)
    }

    /** Returns the region covering [location], or `null` outside the hub world or every region. */
    private fun regionAt(location: Location): Region? =
        if (location.world == hub.hubWorld) hub.regionIndex.regionAt(location.blockX, location.blockY, location.blockZ) else null

    /**
     * Returns the region of the broken block and the [BlockRules] decision for it, or `null` if
     * the block is outside the hub world.
     */
    private fun decide(event: BlockBreakEvent): Pair<Region?, BreakDecision>? {
        val block = event.block
        if (block.world != hub.hubWorld) return null
        val region = hub.regionIndex.regionAt(block.x, block.y, block.z)
        val pending = RegenerationTicker.queue.isPending(BlockPoint(block.x, block.y, block.z))
        return region to BlockRules.decide(region, block.type, pending)
    }
}
