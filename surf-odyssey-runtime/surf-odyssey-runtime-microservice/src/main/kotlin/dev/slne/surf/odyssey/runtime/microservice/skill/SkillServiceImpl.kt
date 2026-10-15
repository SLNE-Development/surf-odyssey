package dev.slne.surf.odyssey.runtime.microservice.skill

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.less
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertIgnore
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.update
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.upsert
import dev.slne.surf.odyssey.core.common.skill.SkillCurve
import dev.slne.surf.odyssey.core.common.skill.SkillProgress
import dev.slne.surf.odyssey.core.common.skill.SkillService
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.microservice.db.table.SkillTable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

object SkillServiceImpl : SkillService {
    override suspend fun loadSkills(playerUuid: SerializableUUID): List<SkillProgress> = suspendTransaction {
        SkillTable.selectAll()
            .where { SkillTable.ownerUuid eq playerUuid }
            .map { SkillProgress(SkillType.valueOf(it[SkillTable.skill]), it[SkillTable.xp]) }
            .toList()
    }

    override suspend fun saveSkill(
        playerUuid: SerializableUUID,
        skill: SkillType,
        xp: Long
    ): Unit = suspendTransaction {
        val clamped = SkillCurve.clamp(xp)

        // Creates the row when missing, so the conditional update below is the only write that can lower nothing
        SkillTable.insertIgnore {
            it[ownerUuid] = playerUuid
            it[SkillTable.skill] = skill.name
            it[SkillTable.xp] = clamped
        }

        // Only raises the stored value, so a late older save never overwrites a newer one
        SkillTable.update({
            (SkillTable.ownerUuid eq playerUuid) and (SkillTable.skill eq skill.name) and (SkillTable.xp less clamped)
        }) {
            it[SkillTable.xp] = clamped
        }
    }

    override suspend fun setSkill(
        playerUuid: SerializableUUID,
        skill: SkillType,
        xp: Long
    ): Unit = suspendTransaction {
        SkillTable.upsert(SkillTable.ownerUuid, SkillTable.skill) {
            it[ownerUuid] = playerUuid
            it[SkillTable.skill] = skill.name
            it[SkillTable.xp] = SkillCurve.clamp(xp)
        }
    }
}