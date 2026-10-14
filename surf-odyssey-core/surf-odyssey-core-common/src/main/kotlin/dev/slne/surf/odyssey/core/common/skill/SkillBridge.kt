package dev.slne.surf.odyssey.core.common.skill

import dev.slne.surf.api.core.util.requiredService
import java.util.*

private val bridge = requiredService<SkillBridge>()

/** Client side access to the [SkillService] of the microservice. */
interface SkillBridge {
    suspend fun loadSkills(playerUuid: UUID): List<SkillProgress>
    suspend fun saveSkill(playerUuid: UUID, skill: SkillType, xp: Long)
    suspend fun setSkill(playerUuid: UUID, skill: SkillType, xp: Long)

    companion object : SkillBridge by bridge {
        val INSTANCE get() = bridge
    }
}
