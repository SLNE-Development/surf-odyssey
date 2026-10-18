package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin

/**
 * A block position that opens a station.
 *
 * @property station the station kind opened at the position
 * @property x the block x coordinate
 * @property y the block y coordinate
 * @property z the block z coordinate
 */
data class StationPlacement(val station: StationDefinition, val x: Int, val y: Int, val z: Int)

/** Turns block positions into stations. */
object StationBlocks {
    /**
     * Registers a listener that opens the station of a placement when a player right-clicks its block, and cancels
     * every interaction with, break of and placement at a placement's block. The listener runs at the highest
     * priority, so these blocks stay protected whatever other region rules decide.
     *
     * @param plugin the plugin that owns the listener
     * @param placements the station positions; positions are matched in every world
     */
    fun install(plugin: Plugin, placements: List<StationPlacement>) {
        plugin.server.pluginManager.registerEvents(StationBlockListener(placements), plugin)
    }
}

/**
 * Opens and protects the blocks of [placements].
 *
 * @param placements the station positions
 */
private class StationBlockListener(placements: List<StationPlacement>) : Listener {
    /** The station at each position. */
    private val stations: Map<Position, StationDefinition> =
        placements.associate { Position(it.x, it.y, it.z) to it.station }

    /** Cancels any interaction with a station block and opens the station on a main-hand right-click. */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInteract(event: PlayerInteractEvent) {
        val station = event.clickedBlock?.let(::stationAt) ?: return
        event.isCancelled = true
        if (event.action == Action.RIGHT_CLICK_BLOCK && event.hand == EquipmentSlot.HAND) {
            StationView.open(event.player, station)
        }
    }

    /** Cancels breaking a station block. */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBreak(event: BlockBreakEvent) {
        if (stationAt(event.block) != null) event.isCancelled = true
    }

    /** Cancels placing a block at a station position. */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlace(event: BlockPlaceEvent) {
        if (stationAt(event.block) != null) event.isCancelled = true
    }

    /** Returns the station at the position of [block], or `null`. */
    private fun stationAt(block: Block): StationDefinition? = stations[Position(block.x, block.y, block.z)]

    /**
     * A block position.
     *
     * @property x the block x coordinate
     * @property y the block y coordinate
     * @property z the block z coordinate
     */
    private data class Position(val x: Int, val y: Int, val z: Int)
}
