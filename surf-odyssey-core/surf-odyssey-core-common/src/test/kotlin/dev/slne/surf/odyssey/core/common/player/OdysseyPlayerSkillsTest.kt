package dev.slne.surf.odyssey.core.common.player

import dev.slne.surf.odyssey.core.common.skill.SkillCurve
import dev.slne.surf.odyssey.core.common.skill.SkillProgress
import dev.slne.surf.odyssey.core.common.skill.SkillType
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OdysseyPlayerSkillsTest {
    private fun player() = OdysseyPlayer(UUID.randomUUID())

    @Test
    fun `no xp is granted before the skills are loaded`() {
        val player = player()

        assertFalse(player.skillsLoaded)
        assertFalse(player.addXp(SkillType.MINING, 10))
        assertEquals(0L, player.xp(SkillType.MINING))
    }

    @Test
    fun `loaded skills set the xp and enable granting`() {
        val player = player()

        player.applySkills(listOf(SkillProgress(SkillType.MINING, 50)))

        assertTrue(player.skillsLoaded)
        assertEquals(50L, player.xp(SkillType.MINING))
        assertEquals(0L, player.xp(SkillType.FISHING))
        assertTrue(player.addXp(SkillType.MINING, 5))
        assertEquals(55L, player.xp(SkillType.MINING))
    }

    @Test
    fun `non positive amounts are ignored`() {
        val player = player().apply { applySkills(emptyList()) }

        assertFalse(player.addXp(SkillType.FISHING, 0))
        assertFalse(player.addXp(SkillType.FISHING, -3))
        assertEquals(0L, player.xp(SkillType.FISHING))
    }

    @Test
    fun `xp is capped at the max xp and never overflows`() {
        val player = player().apply { applySkills(emptyList()) }

        assertTrue(player.addXp(SkillType.MOB_HUNTING, Long.MAX_VALUE))
        assertTrue(player.addXp(SkillType.MOB_HUNTING, Long.MAX_VALUE))

        assertEquals(SkillCurve.MAX_XP, player.xp(SkillType.MOB_HUNTING))
        assertEquals(SkillCurve.MAX_LEVEL, player.level(SkillType.MOB_HUNTING))
    }

    @Test
    fun `set xp replaces the value and is clamped`() {
        val player = player().apply { applySkills(listOf(SkillProgress(SkillType.MINING, 500))) }

        player.setXp(SkillType.MINING, 20)
        assertEquals(20L, player.xp(SkillType.MINING))

        player.setXp(SkillType.MINING, -4)
        assertEquals(0L, player.xp(SkillType.MINING))
    }

    @Test
    fun `concurrent grants do not lose xp`() {
        val player = player().apply { applySkills(emptyList()) }
        val threads = List(8) {
            Thread { repeat(1_000) { player.addXp(SkillType.MINING, 1) } }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(8_000L, player.xp(SkillType.MINING))
    }
}
