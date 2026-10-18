package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks how many mobs [MobSpawnPlanner] spawns per interval. */
class MobSpawnPlannerTest {
    @Test
    fun `nothing spawns before the interval has passed`() =
        assertEquals(0, MobSpawnPlanner.toSpawn(alive = 0, maxAlive = 5, lastSpawnAt = 1_000, now = 5_000, intervalMillis = 10_000))

    @Test
    fun `one mob spawns per interval while below the maximum`() =
        assertEquals(1, MobSpawnPlanner.toSpawn(alive = 2, maxAlive = 5, lastSpawnAt = 0, now = 10_000, intervalMillis = 10_000))

    @Test
    fun `the maximum is never exceeded`() =
        assertEquals(0, MobSpawnPlanner.toSpawn(alive = 5, maxAlive = 5, lastSpawnAt = 0, now = 99_000, intervalMillis = 1_000))
}
