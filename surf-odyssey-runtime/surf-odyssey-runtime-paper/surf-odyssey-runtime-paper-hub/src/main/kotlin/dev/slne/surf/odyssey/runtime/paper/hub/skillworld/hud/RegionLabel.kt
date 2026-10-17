package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.hud

import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.region.SkillRegions

enum class WorldKind {
    HUB,

    SKILL_WORLD,

    OTHER,
}

enum class RegionLabel(val text: String) {
    HUB("Hub"),

    SKILL_WORLD("Skill Welt"),

    MINING("Mining"),

    FISHING("Fishing"),

    MOB_HUNTING("Mobhunting"),
}

private val SKILL_LABELS = listOf(
    SkillType.MINING to RegionLabel.MINING,
    SkillType.FISHING to RegionLabel.FISHING,
    SkillType.MOB_HUNTING to RegionLabel.MOB_HUNTING,
)

fun resolveRegionLabel(kind: WorldKind, regions: SkillRegions, x: Int, y: Int, z: Int): RegionLabel? = when (kind) {
    WorldKind.HUB -> RegionLabel.HUB
    WorldKind.OTHER -> null
    WorldKind.SKILL_WORLD -> SKILL_LABELS.firstOrNull { (skill, _) -> regions.allows(skill, x, y, z) }?.second
        ?: RegionLabel.SKILL_WORLD
}
