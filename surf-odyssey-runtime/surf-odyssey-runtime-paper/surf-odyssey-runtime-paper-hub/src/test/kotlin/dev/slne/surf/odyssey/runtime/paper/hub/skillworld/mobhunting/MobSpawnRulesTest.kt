package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mobhunting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MobSpawnRulesTest {
    @Test
    fun `spawns the missing mobs up to the maximum`() {
        assertEquals(4, MobSpawnRules.missing(alive = 6, max = 10))
        assertEquals(0, MobSpawnRules.missing(alive = 10, max = 10))
        assertEquals(0, MobSpawnRules.missing(alive = 12, max = 10))
        assertEquals(0, MobSpawnRules.missing(alive = 0, max = -1))
    }

    @Test
    fun `mobs outside their region go back`() {
        assertTrue(MobSpawnRules.mustReturn(inRegion = false))
        assertFalse(MobSpawnRules.mustReturn(inRegion = true))
    }

    @Test
    fun `spawns only into loaded chunks with loaded entities`() {
        assertTrue(MobSpawnRules.canSpawnAt(chunkLoaded = true, entitiesLoaded = true))
        assertFalse(MobSpawnRules.canSpawnAt(chunkLoaded = true, entitiesLoaded = false))
        assertFalse(MobSpawnRules.canSpawnAt(chunkLoaded = false, entitiesLoaded = true))
        assertFalse(MobSpawnRules.canSpawnAt(chunkLoaded = false, entitiesLoaded = false))
    }
}
