package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks how [AliveMobs] counts, prunes and removes the mobs of a spawn region. */
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
    fun `removing all mobs hands each one over and stops counting them`() {
        val mobs = AliveMobs<Int>()
        listOf(1, 2, 3).forEach(mobs::add)
        val removed = mutableListOf<Int>()
        mobs.removeAll { removed.add(it) }
        assertEquals(setOf(1, 2, 3), removed.toSet())
        assertEquals(0, mobs.count)
    }

    @Test
    fun `a freed slot lets the planner spawn again`() {
        val mobs = AliveMobs<Int>()
        listOf(1, 2).forEach(mobs::add)
        assertEquals(0, MobSpawnPlanner.toSpawn(mobs.count, 2, 0, 10_000, 1_000))
        mobs.prune { it == 1 }
        assertEquals(1, MobSpawnPlanner.toSpawn(mobs.count, 2, 0, 10_000, 1_000))
    }

    @Test
    fun `tracked mobs are known until they are pruned`() {
        val mobs = AliveMobs<Int>()
        mobs.add(1)
        assertTrue(1 in mobs)
        assertFalse(2 in mobs)
        mobs.prune { false }
        assertFalse(1 in mobs)
    }
}
