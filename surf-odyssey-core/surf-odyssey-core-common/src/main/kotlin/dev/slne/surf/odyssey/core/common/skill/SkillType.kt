package dev.slne.surf.odyssey.core.common.skill

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SkillType {
    @SerialName("mining")
    MINING,

    @SerialName("fishing")
    FISHING,

    @SerialName("mob_hunting")
    MOB_HUNTING
}