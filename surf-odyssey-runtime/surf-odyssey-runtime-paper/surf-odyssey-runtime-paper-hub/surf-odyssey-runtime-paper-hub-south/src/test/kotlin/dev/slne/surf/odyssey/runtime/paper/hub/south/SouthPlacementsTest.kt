package dev.slne.surf.odyssey.runtime.paper.hub.south

import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionPlacements
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests where South places its NPCs. */
class SouthPlacementsTest {
    @Test
    fun `ysa stands on the platform outside the portal`() {
        val placement = ProgressionPlacements().apply { southPlacements() }.npcs.single()
        assertEquals("ysa", placement.npc.id)
        val (x, y, z) = Triple(floor(placement.position.x).toInt(), floor(placement.position.y).toInt(), floor(placement.position.z).toInt())
        assertEquals(64, y)
        assertTrue(x in -5..5 && z in -1..7, "ysa stands on the platform")
        southPortals().forEach { assertFalse(it.shape.contains(x, y, z), "ysa stands in ${it.id}") }
    }
}
