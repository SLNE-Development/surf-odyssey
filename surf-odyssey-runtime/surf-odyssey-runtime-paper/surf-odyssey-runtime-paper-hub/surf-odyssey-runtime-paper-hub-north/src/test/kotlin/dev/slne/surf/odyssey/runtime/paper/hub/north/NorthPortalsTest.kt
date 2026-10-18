package dev.slne.surf.odyssey.runtime.paper.hub.north

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Tests the portals North declares. */
class NorthPortalsTest {
    /** North has one portal, back to the lobby. */
    @Test
    fun `north has a portal to the lobby`() {
        assertEquals(listOf("odyssey-hub-lobby"), northPortals().map { it.targetCategory })
    }

    /** Neither the portal blocks nor the arrival point lie in a region, and the arrival point is outside the portal. */
    @Test
    fun `portal blocks and arrival points lie outside every region and arrivals outside their portal`() {
        val regions = RegionRegistry().apply { northRegions() }.build()
        northPortals().forEach { portal ->
            val a = portal.arrival
            val (x, y, z) = Triple(floor(a.x).toInt(), floor(a.y).toInt(), floor(a.z).toInt())
            assertFalse(portal.shape.contains(x, y, z), "arrival of ${portal.id} lies inside its portal")
            regions.forEach {
                assertFalse(it.shape.contains(x, y, z), "arrival of ${portal.id} lies in ${it.id}")
                val b = portal.shape.bounds
                for (px in b.minX..b.maxX) for (py in b.minY..b.maxY) for (pz in b.minZ..b.maxZ) {
                    assertFalse(portal.shape.contains(px, py, pz) && it.shape.contains(px, py, pz), "${portal.id} overlaps ${it.id}")
                }
            }
        }
    }
}
