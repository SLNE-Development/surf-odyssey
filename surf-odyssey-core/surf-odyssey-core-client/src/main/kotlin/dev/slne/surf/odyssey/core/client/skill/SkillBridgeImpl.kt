package dev.slne.surf.odyssey.core.client.skill

import com.google.auto.service.AutoService
import dev.slne.surf.odyssey.core.client.OdysseyClientInstance
import dev.slne.surf.odyssey.core.common.skill.SkillBridge
import dev.slne.surf.odyssey.core.common.skill.SkillProgress
import dev.slne.surf.odyssey.core.common.skill.SkillService
import dev.slne.surf.odyssey.core.common.skill.SkillType
import java.util.*

private val proxy by lazy {
    OdysseyClientInstance.INSTANCE.rabbitApi.createRpcService<SkillService>()
}

@AutoService(SkillBridge::class)
class SkillBridgeImpl : SkillBridge {
    override suspend fun loadSkills(playerUuid: UUID): List<SkillProgress> {
        return proxy.loadSkills(playerUuid)
    }

    override suspend fun saveSkill(playerUuid: UUID, skill: SkillType, xp: Long) {
        proxy.saveSkill(playerUuid, skill, xp)
    }

    override suspend fun setSkill(playerUuid: UUID, skill: SkillType, xp: Long) {
        proxy.setSkill(playerUuid, skill, xp)
    }
}
