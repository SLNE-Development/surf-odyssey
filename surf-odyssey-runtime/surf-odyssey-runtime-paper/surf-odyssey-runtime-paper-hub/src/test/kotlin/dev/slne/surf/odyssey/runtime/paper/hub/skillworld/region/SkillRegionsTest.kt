package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.region

import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.BlockPosConfig
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.BoxConfig
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.SkillWorldConfig
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockBox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkillRegionsTest {
    private val regions = SkillRegions(
        mapOf(
            SkillType.MINING to BlockBox(0, 0, 0, 10, 10, 10),
            SkillType.FISHING to BlockBox(20, 0, 0, 30, 10, 10),
        )
    )

    @Test
    fun `allows a skill inside its own region including the border`() {
        assertTrue(regions.allows(SkillType.MINING, 0, 0, 0))
        assertTrue(regions.allows(SkillType.MINING, 10, 10, 10))
    }

    @Test
    fun `denies a skill inside another skill's region`() {
        assertFalse(regions.allows(SkillType.MINING, 25, 5, 5))
    }

    @Test
    fun `denies a skill without a region`() {
        assertFalse(regions.allows(SkillType.MOB_HUNTING, 5, 5, 5))
        assertNull(regions.box(SkillType.MOB_HUNTING))
    }

    @Test
    fun `denies outside every region`() {
        assertFalse(regions.allows(SkillType.FISHING, 15, 5, 5))
        assertEquals(BlockBox(20, 0, 0, 30, 10, 10), regions.box(SkillType.FISHING))
    }

    @Test
    fun `builds a region from config with swapped corners`() {
        val config = SkillWorldConfig(
            regions = mapOf(
                SkillType.MINING to BoxConfig(
                    min = BlockPosConfig(10, 10, 10),
                    max = BlockPosConfig(0, 0, 0),
                ),
            ),
        )
        val fromConfig = SkillRegions.from(config)

        assertTrue(fromConfig.allows(SkillType.MINING, 0, 0, 0))
        assertTrue(fromConfig.allows(SkillType.MINING, 10, 10, 10))
        assertEquals(BlockBox(0, 0, 0, 10, 10, 10), fromConfig.box(SkillType.MINING))
    }
}
