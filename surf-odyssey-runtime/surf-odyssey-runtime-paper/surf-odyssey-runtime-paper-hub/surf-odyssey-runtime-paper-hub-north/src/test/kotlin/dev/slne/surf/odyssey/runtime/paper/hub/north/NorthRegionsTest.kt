package dev.slne.surf.odyssey.runtime.paper.hub.north

import dev.slne.surf.odyssey.runtime.paper.hub.common.fishing.FishingRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.mobs.MobSpawnRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.ForestRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.MineRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionIndex
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests the regions North declares. */
class NorthRegionsTest {
    /** North declares eight regions that share no block. */
    @Test
    fun `north declares three forests, two mines, one lake, its ruins and a wolf area without overlaps`() {
        val index = RegionIndex(RegionRegistry().apply { northRegions() }.build())
        assertEquals(8, index.regions.size)
        assertEquals(3, index.regions.count { it is ForestRegion })
        assertEquals(2, index.regions.count { it is MineRegion })
        assertEquals(1, index.regions.count { it is FishingRegion })
        assertEquals(1, index.regions.count { it is NorthRuinsRegion })
        assertEquals(1, index.regions.count { it is MobSpawnRegion && it.id == "north_wolves" })
    }

    /** The house next to the oak forest is protected while the canopy around and above it is part of the forest. */
    @Test
    fun `oak forest excludes the house body but covers the space above its roof`() {
        val oak = RegionIndex(RegionRegistry().apply { northRegions() }.build()).byId("north_forest_oak")!!
        assertFalse(oak.shape.contains(365, 65, 130))
        assertFalse(oak.shape.contains(362, 63, 127))
        assertFalse(oak.shape.contains(368, 67, 133))
        assertTrue(oak.shape.contains(365, 68, 130))
        assertTrue(oak.shape.contains(361, 65, 130))
    }
}
