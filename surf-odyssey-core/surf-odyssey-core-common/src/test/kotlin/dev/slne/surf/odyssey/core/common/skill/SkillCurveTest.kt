package dev.slne.surf.odyssey.core.common.skill

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkillCurveTest {
    @Test
    fun `level one needs no xp`() {
        assertEquals(0L, SkillCurve.xpForLevel(1))
        assertEquals(1, SkillCurve.levelFor(0))
    }

    @Test
    fun `xp for level strictly increases`() {
        for (level in 2..SkillCurve.MAX_LEVEL) {
            assertTrue(SkillCurve.xpForLevel(level) > SkillCurve.xpForLevel(level - 1), "level $level")
        }
    }

    @Test
    fun `level boundaries are exact`() {
        for (level in 1..SkillCurve.MAX_LEVEL) {
            assertEquals(level, SkillCurve.levelFor(SkillCurve.xpForLevel(level)), "at level $level")
        }
        for (level in 2..SkillCurve.MAX_LEVEL) {
            assertEquals(level - 1, SkillCurve.levelFor(SkillCurve.xpForLevel(level) - 1), "below level $level")
        }
    }

    @Test
    fun `negative xp is level one and huge xp is max level`() {
        assertEquals(1, SkillCurve.levelFor(-5))
        assertEquals(SkillCurve.MAX_LEVEL, SkillCurve.levelFor(Long.MAX_VALUE))
    }

    @Test
    fun `clamp keeps xp inside zero and max xp`() {
        assertEquals(0L, SkillCurve.clamp(-1))
        assertEquals(SkillCurve.MAX_XP, SkillCurve.clamp(Long.MAX_VALUE))
        assertEquals(42L, SkillCurve.clamp(42))
    }

    @Test
    fun `progress derives its level from the curve`() {
        val progress = SkillProgress(SkillType.MINING, SkillCurve.xpForLevel(3))
        assertEquals(3, progress.level)
    }
}
