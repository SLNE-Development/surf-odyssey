package dev.slne.surf.odyssey.runtime.paper.progression

import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.npc.NpcPlacement

/**
 * A position with a view direction in the main world of a server.
 *
 * @property x the x coordinate
 * @property y the y coordinate
 * @property z the z coordinate
 * @property yaw the horizontal view direction in degrees
 * @property pitch the vertical view direction in degrees
 */
data class SpawnPoint(val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float)

/**
 * The position of a block in the main world of a server.
 *
 * @property x the x coordinate
 * @property y the y coordinate
 * @property z the z coordinate
 */
data class BlockPos(val x: Int, val y: Int, val z: Int)

/**
 * A station kind placed on a block.
 *
 * @property station the station kind
 * @property position the block that opens the station
 */
data class StationPlacement(val station: StationDefinition, val position: BlockPos)

/**
 * Collects where the NPCs and stations of a server stand. Each call adds one placement; the same definition may be
 * placed more than once.
 */
class ProgressionPlacements {
    /** The NPC placements added so far, in the order they were added. */
    private val npcPlacements = mutableListOf<NpcPlacement>()

    /** The station placements added so far, in the order they were added. */
    private val stationPlacements = mutableListOf<StationPlacement>()

    /** The NPC placements, in the order they were added. */
    val npcs: List<NpcPlacement>
        get() = npcPlacements.toList()

    /** The station placements, in the order they were added. */
    val stations: List<StationPlacement>
        get() = stationPlacements.toList()

    /**
     * Places an NPC.
     *
     * @param definition the NPC
     * @param at where the NPC stands and which way it looks
     */
    fun npc(definition: NpcDefinition, at: SpawnPoint) {
        npcPlacements += NpcPlacement(definition, at)
    }

    /**
     * Places a station on a block.
     *
     * @param definition the station kind
     * @param at the block that opens the station
     */
    fun station(definition: StationDefinition, at: BlockPos) {
        stationPlacements += StationPlacement(definition, at)
    }
}
