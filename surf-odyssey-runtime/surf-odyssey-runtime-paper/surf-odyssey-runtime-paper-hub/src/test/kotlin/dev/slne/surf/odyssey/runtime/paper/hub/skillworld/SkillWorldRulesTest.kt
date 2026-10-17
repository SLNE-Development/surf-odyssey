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
    fun `an unload is judged by the world being present in Bukkit`() {
        assertEquals(SkillWorldRules.UnloadOutcome.Gone, SkillWorldRules.unloadOutcome(worldStillInBukkit = false))
        assertEquals(SkillWorldRules.UnloadOutcome.StillPresent, SkillWorldRules.unloadOutcome(worldStillInBukkit = true))
    }

    @Test
    fun `no name or an empty name selects the build world`() {
        assertEquals(SkillWorldRules.WorldMode.Build, SkillWorldRules.worldMode(null, sourceExists = true, reservedNames = emptySet()))
        assertEquals(SkillWorldRules.WorldMode.Build, SkillWorldRules.worldMode("  ", sourceExists = true, reservedNames = emptySet()))
    }

    @Test
    fun `an existing source selects live mode and a missing one the build world`() {
        assertEquals(SkillWorldRules.WorldMode.Live, SkillWorldRules.worldMode("odyssey_skills", sourceExists = true, reservedNames = emptySet()))
        assertEquals(SkillWorldRules.WorldMode.Build, SkillWorldRules.worldMode("odyssey_skills", sourceExists = false, reservedNames = emptySet()))
        assertEquals(SkillWorldRules.WorldMode.Live, SkillWorldRules.worldMode(SkillWorldRules.BUILD_WORLD_NAME, sourceExists = true, reservedNames = emptySet()))
    }

    @Test
    fun `names that could leave the world folder or hit reserved worlds are invalid`() {
        val reserved = setOf("world")
        listOf("../world", "a/b", "a\\b", "a b", "x".repeat(33), "skills_runtime", "world").forEach { name ->
            assertEquals(SkillWorldRules.WorldMode.Invalid, SkillWorldRules.worldMode(name, sourceExists = true, reservedNames = reserved), name)
        }
    }

    @Test
    fun `the runtime world is named after the source`() {
        assertEquals("skill_world_runtime", SkillWorldRules.runtimeName(SkillWorldRules.BUILD_WORLD_NAME))
    }
}
