package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.hud

import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockBox
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.region.SkillRegions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegionLabelTest {
    private val regions = SkillRegions(
        mapOf(
            SkillType.MINING to BlockBox(0, 0, 0, 10, 10, 10),
            SkillType.FISHING to BlockBox(20, 0, 0, 30, 10, 10),
            SkillType.MOB_HUNTING to BlockBox(40, 0, 0, 50, 10, 10),
        )
    )

    @Test
    fun `hub world is always hub`() {
        assertEquals(RegionLabel.HUB, resolveRegionLabel(WorldKind.HUB, regions, 5, 5, 5))
    }

    @Test
    fun `other worlds have no label`() {
        assertNull(resolveRegionLabel(WorldKind.OTHER, regions, 5, 5, 5))
    }

    @Test
    fun `skill world resolves each skill region`() {
        assertEquals(RegionLabel.MINING, resolveRegionLabel(WorldKind.SKILL_WORLD, regions, 5, 5, 5))
        assertEquals(RegionLabel.FISHING, resolveRegionLabel(WorldKind.SKILL_WORLD, regions, 25, 5, 5))
        assertEquals(RegionLabel.MOB_HUNTING, resolveRegionLabel(WorldKind.SKILL_WORLD, regions, 45, 5, 5))
    }

    @Test
    fun `region border counts as inside`() {
        assertEquals(RegionLabel.MINING, resolveRegionLabel(WorldKind.SKILL_WORLD, regions, 0, 0, 0))
        assertEquals(RegionLabel.MINING, resolveRegionLabel(WorldKind.SKILL_WORLD, regions, 10, 10, 10))
    }

    @Test
    fun `skill world outside every region is skill world`() {
        assertEquals(RegionLabel.SKILL_WORLD, resolveRegionLabel(WorldKind.SKILL_WORLD, regions, 15, 5, 5))
    }

    @Test
    fun `skill world without configured regions is skill world`() {
        assertEquals(RegionLabel.SKILL_WORLD, resolveRegionLabel(WorldKind.SKILL_WORLD, SkillRegions(emptyMap()), 5, 5, 5))
    }

    @Test
    fun `overlapping regions resolve in the fixed order mining, fishing, mob hunting`() {
        val overlapping = SkillRegions(
            mapOf(
                SkillType.MOB_HUNTING to BlockBox(0, 0, 0, 10, 10, 10),
                SkillType.FISHING to BlockBox(0, 0, 0, 10, 10, 10),
                SkillType.MINING to BlockBox(0, 0, 0, 10, 10, 10),
            )
        )
        assertEquals(RegionLabel.MINING, resolveRegionLabel(WorldKind.SKILL_WORLD, overlapping, 5, 5, 5))

        val noMining = SkillRegions(
            mapOf(
                SkillType.MOB_HUNTING to BlockBox(0, 0, 0, 10, 10, 10),
                SkillType.FISHING to BlockBox(0, 0, 0, 10, 10, 10),
            )
        )
        assertEquals(RegionLabel.FISHING, resolveRegionLabel(WorldKind.SKILL_WORLD, noMining, 5, 5, 5))
    }

    @Test
    fun `label texts are the displayed names`() {
        assertEquals(
            listOf("Hub", "Skill Welt", "Mining", "Fishing", "Mobhunting"),
            RegionLabel.entries.map { it.text },
        )
    }
}
