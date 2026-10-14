package dev.slne.surf.odyssey.core.common.skill

import kotlinx.serialization.Serializable

@Serializable
data class SkillProgress(
    val skill: SkillType,
    val xp: Long
) {
    val level: Int get() = SkillCurve.levelFor(xp)
}
