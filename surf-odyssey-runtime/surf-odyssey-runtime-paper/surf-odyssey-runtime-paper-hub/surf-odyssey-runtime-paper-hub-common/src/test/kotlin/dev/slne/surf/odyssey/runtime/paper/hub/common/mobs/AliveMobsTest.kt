package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import kotlin.test.Test
import kotlin.test.assertEquals

class AliveMobsTest {
    @Test
    fun `tracked mobs are counted`() {
        val mobs = AliveMobs<Int>()
        mobs.add(1)
        mobs.add(2)
        assertEquals(2, mobs.count)
    }

    @Test
    fun `dead or removed mobs are freed on prune`() {
        val mobs = AliveMobs<Int>()
        listOf(1, 2, 3).forEach(mobs::add)
        mobs.prune { it == 2 }
        assertEquals(1, mobs.count)
    }

    @Test
    fun `a freed slot lets the planner spawn again`() {
        val mobs = AliveMobs<Int>()
        listOf(1, 2).forEach(mobs::add)
        assertEquals(0, MobSpawnPlanner.toSpawn(mobs.count, 2, 0, 10_000, 1_000))
        mobs.prune { it == 1 }
        assertEquals(1, MobSpawnPlanner.toSpawn(mobs.count, 2, 0, 10_000, 1_000))
    }
}
