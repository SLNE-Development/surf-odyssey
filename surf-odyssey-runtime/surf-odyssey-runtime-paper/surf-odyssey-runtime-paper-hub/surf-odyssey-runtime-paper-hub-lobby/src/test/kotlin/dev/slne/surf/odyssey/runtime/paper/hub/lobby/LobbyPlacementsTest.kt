package dev.slne.surf.odyssey.runtime.paper.hub.lobby

import dev.slne.surf.odyssey.runtime.paper.progression.BlockPos
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionPlacements
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests where the lobby places its NPCs and its workbench. */
class LobbyPlacementsTest {
    /** The placements of the lobby. */
    private val placements = ProgressionPlacements().apply { lobbyPlacements() }

    @Test
    fun `brina and the cook stand on the platform near the spawn outside the portals`() {
        assertEquals(listOf("brina", "koch"), placements.npcs.map { it.npc.id })
        placements.npcs.forEach { placement ->
            val (x, y, z) = Triple(floor(placement.position.x).toInt(), floor(placement.position.y).toInt(), floor(placement.position.z).toInt())
            assertEquals(64, y, "${placement.npc.id} stands on the platform")
            assertTrue(x in -12..12 && z in -8..8, "${placement.npc.id} stands on the platform")
            assertTrue(x * x + z * z in 4..64, "${placement.npc.id} stands near the spawn")
            lobbyPortals().forEach { assertFalse(it.shape.contains(x, y, z), "${placement.npc.id} stands in ${it.id}") }
        }
    }

    @Test
    fun `the workbench is a crafting table on the platform in front of the spawn`() {
        val station = placements.stations.single()
        assertEquals("workbench", station.station.id)
        assertEquals(BlockPos(0, 64, 5), BlockPos(station.x, station.y, station.z))
    }
}
