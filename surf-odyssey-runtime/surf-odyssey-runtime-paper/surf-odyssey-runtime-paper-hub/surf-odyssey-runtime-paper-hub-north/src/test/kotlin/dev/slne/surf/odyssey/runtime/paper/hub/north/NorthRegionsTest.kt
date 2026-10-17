package dev.slne.surf.odyssey.runtime.paper.hub.north

import dev.slne.surf.odyssey.runtime.paper.hub.common.fishing.FishingRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.mobs.MobSpawnRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.ForestRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.MineRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionIndex
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
