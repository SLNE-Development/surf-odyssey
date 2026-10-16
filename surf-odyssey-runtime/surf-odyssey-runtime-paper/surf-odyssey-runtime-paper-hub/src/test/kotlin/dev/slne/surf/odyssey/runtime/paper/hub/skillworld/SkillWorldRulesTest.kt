package dev.slne.surf.odyssey.runtime.paper.hub.skillworld

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SkillWorldRulesTest {
    @Test
    fun `breaking is allowed only inside the mining region and never for unbreakable blocks`() {
        assertTrue(SkillWorldRules.canBreak(inMiningRegion = true, unbreakable = false))
        assertFalse(SkillWorldRules.canBreak(inMiningRegion = true, unbreakable = true))
        assertFalse(SkillWorldRules.canBreak(inMiningRegion = false, unbreakable = false))
    }

    @Test
    fun `buckets are cancelled in the skill world only`() {
        assertTrue(SkillWorldRules.mustCancelBucket(inSkillWorld = true))
        assertFalse(SkillWorldRules.mustCancelBucket(inSkillWorld = false))
    }

    @Test
    fun `only custom creature spawns are allowed in the skill world`() {
        assertTrue(SkillWorldRules.mustCancelCreatureSpawn(inSkillWorld = true, custom = false))
        assertFalse(SkillWorldRules.mustCancelCreatureSpawn(inSkillWorld = true, custom = true))
        assertFalse(SkillWorldRules.mustCancelCreatureSpawn(inSkillWorld = false, custom = false))
    }

    @Test
    fun `players are ejected from a locked region only`() {
        assertTrue(SkillWorldRules.mustEjectFromLockedRegion(locked = true, inRegion = true))
        assertFalse(SkillWorldRules.mustEjectFromLockedRegion(locked = true, inRegion = false))
        assertFalse(SkillWorldRules.mustEjectFromLockedRegion(locked = false, inRegion = true))
    }

    @Test
    fun `an unload is judged by the world being present, not by what polar reported`() {
        assertEquals(SkillWorldRules.UnloadOutcome.Gone, SkillWorldRules.unloadOutcome(worldStillInBukkit = false))
        assertEquals(SkillWorldRules.UnloadOutcome.StillPresent, SkillWorldRules.unloadOutcome(worldStillInBukkit = true))
    }
}
