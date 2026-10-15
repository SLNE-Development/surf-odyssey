package dev.slne.surf.odyssey.core.common.skill

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.rabbitmq.api.rpc.RpcService

@RpcService
interface SkillService {
    suspend fun loadSkills(playerUuid: SerializableUUID): List<SkillProgress>

    suspend fun saveSkill(playerUuid: SerializableUUID, skill: SkillType, xp: Long)

    suspend fun setSkill(playerUuid: SerializableUUID, skill: SkillType, xp: Long)
}
