package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.region

import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.SkillWorldConfig
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockBox

class SkillRegions(private val boxes: Map<SkillType, BlockBox>) {
    fun allows(skill: SkillType, x: Int, y: Int, z: Int): Boolean = boxes[skill]?.contains(x, y, z) == true

    fun box(skill: SkillType): BlockBox? = boxes[skill]

    companion object {
        fun from(config: SkillWorldConfig) = SkillRegions(config.regions.mapValues { it.value.toBox() })
    }
}
