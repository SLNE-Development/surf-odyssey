package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks where [MobSpawnPlanner.spawnPosition] puts a mob relative to its spawn point. */
class MobSpawnPlannerSpawnPositionTest {
    @Test
    fun `a mob stands centred on top of its spawn block`() =
        assertEquals(Triple(3.5, 65.0, -6.5), MobSpawnPlanner.spawnPosition(BlockPoint(3, 64, -7)))
}
