package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * A block position that opens a station.
 *
 * @property station the station kind opened at the position
 * @property x the block x coordinate
 * @property y the block y coordinate
 * @property z the block z coordinate
 */
data class StationPlacement(val station: StationDefinition, val x: Int, val y: Int, val z: Int)

/**
 * Finds the station placed at a block position of one world.
 *
 * @param worldId the id of the world the placements belong to
 * @param placements the station positions in that world
 */
class StationPositions(private val worldId: UUID, placements: List<StationPlacement>) {
    /** The station at each position. */
    private val stations: Map<Position, StationDefinition> =
        placements.associate { Position(it.x, it.y, it.z) to it.station }

    /**
     * Returns the station at the block position [x], [y], [z] of the world [world].
     *
     * @return the station, or `null` if [world] is not the world of the placements or no station is placed there
     */
    fun stationAt(world: UUID, x: Int, y: Int, z: Int): StationDefinition? =
        if (world != worldId) null else stations[Position(x, y, z)]

    /**
     * A block position.
     *
     * @property x the block x coordinate
     * @property y the block y coordinate
     * @property z the block z coordinate
     */
    private data class Position(val x: Int, val y: Int, val z: Int)
}

/** Turns block positions into stations. */
object StationBlocks {
    /**
     * Registers a listener that opens the station of a placement when a player right-clicks its block in [world],
     * and cancels every interaction with, break of and placement at a placement's block there. Blocks at the same
     * positions in other worlds are left alone. The listener runs at the highest priority, so these blocks stay
     * protected whatever other region rules decide.
     *
     * @param plugin the plugin that owns the listener
     * @param world the world the placements are in
     * @param placements the station positions
     */
    fun install(plugin: Plugin, world: World, placements: List<StationPlacement>) {
        plugin.server.pluginManager.registerEvents(
            StationBlockListener(StationPositions(world.uid, placements)), plugin
        )
    }
}

/**
 * Opens and protects the station blocks of [positions].
 *
 * @param positions the station positions
 */
private class StationBlockListener(private val positions: StationPositions) : Listener {
    /**
     * Cancels any interaction with a station block and opens the station on a main-hand right-click that no other
     * listener denied before.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInteract(event: PlayerInteractEvent) {
        val station = event.clickedBlock?.let(::stationAt) ?: return
        val alreadyDenied = event.useInteractedBlock() == Event.Result.DENY
        event.isCancelled = true
        if (StationClicks.opens(event.action, event.hand, alreadyDenied)) StationView.open(event.player, station)
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

    /** Returns the station at [block], or `null`. */
    private fun stationAt(block: Block): StationDefinition? = positions.stationAt(block.world.uid, block.x, block.y, block.z)
}

/** Decides which clicks on a station block open the station. */
internal object StationClicks {
    /**
     * Returns whether a click opens the station: only a right-click on the block with the main hand that no other
     * listener denied.
     *
     * @param action the kind of click
     * @param hand the hand that clicked, or `null` if unknown
     * @param alreadyDenied whether another listener already denied using the block
     */
    fun opens(action: Action, hand: EquipmentSlot?, alreadyDenied: Boolean): Boolean =
        !alreadyDenied && action == Action.RIGHT_CLICK_BLOCK && hand == EquipmentSlot.HAND
}
