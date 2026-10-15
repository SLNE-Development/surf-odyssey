package dev.slne.surf.odyssey.core.common.skill

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkillCurveTest {
    @Test
    fun `level zero needs no xp`() {
        assertEquals(0L, SkillCurve.xpForLevel(0))
        assertEquals(0, SkillCurve.levelFor(0))
    }

    @Test
    fun `xp for level strictly increases`() {
        for (level in 1..SkillCurve.MAX_LEVEL) {
            assertTrue(SkillCurve.xpForLevel(level) > SkillCurve.xpForLevel(level - 1), "level $level")
        }
    }

    @Test
    fun `level boundaries are exact`() {
        for (level in 0..SkillCurve.MAX_LEVEL) {
            assertEquals(level, SkillCurve.levelFor(SkillCurve.xpForLevel(level)), "at level $level")
        }
        for (level in 1..SkillCurve.MAX_LEVEL) {
            assertEquals(level - 1, SkillCurve.levelFor(SkillCurve.xpForLevel(level) - 1), "below level $level")
        }
    }

    @Test
    fun `negative xp is level zero and huge xp is max level`() {
        assertEquals(0, SkillCurve.levelFor(-5))
        assertEquals(SkillCurve.MAX_LEVEL, SkillCurve.levelFor(Long.MAX_VALUE))
    }

    @Test
    fun `clamp only removes negative xp and has no upper limit`() {
        assertEquals(0L, SkillCurve.clamp(-1))
        assertEquals(Long.MAX_VALUE, SkillCurve.clamp(Long.MAX_VALUE))
        assertEquals(SkillCurve.MAX_LEVEL_XP + 1, SkillCurve.clamp(SkillCurve.MAX_LEVEL_XP + 1))
        assertEquals(42L, SkillCurve.clamp(42))
    }

    @Test
    fun `progress derives its level from the curve`() {
        val progress = SkillProgress(SkillType.MINING, SkillCurve.xpForLevel(3))
        assertEquals(3, progress.level)
    }

    @Test
    fun `progress to the next level runs from zero to just below one`() {
        assertEquals(0.0, SkillCurve.progressToNextLevel(0))

        val halfway = (SkillCurve.xpForLevel(2) + SkillCurve.xpForLevel(3)) / 2
        assertTrue(SkillCurve.progressToNextLevel(halfway) in 0.4..0.6)

        assertTrue(SkillCurve.progressToNextLevel(SkillCurve.xpForLevel(3) - 1) < 1.0)
        assertEquals(1.0, SkillCurve.progressToNextLevel(SkillCurve.MAX_LEVEL_XP))
        assertEquals(1.0, SkillCurve.progressToNextLevel(SkillCurve.MAX_LEVEL_XP + 1_000_000))
    }
}
