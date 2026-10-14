package dev.slne.surf.odyssey.core.common.skill

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.rabbitmq.api.rpc.RpcService

/** Stores the skill experience of players in the microservice. */
@RpcService
interface SkillService {
    /** The stored experience of every skill the player has gained experience in. */
    suspend fun loadSkills(playerUuid: SerializableUUID): List<SkillProgress>

    /** Stores [xp] only if it is greater than the stored value, so a late older save never lowers it. */
    suspend fun saveSkill(playerUuid: SerializableUUID, skill: SkillType, xp: Long)

    /** Sets [xp] absolutely, also lowering the stored value. Only for admin corrections. */
    suspend fun setSkill(playerUuid: SerializableUUID, skill: SkillType, xp: Long)
}
