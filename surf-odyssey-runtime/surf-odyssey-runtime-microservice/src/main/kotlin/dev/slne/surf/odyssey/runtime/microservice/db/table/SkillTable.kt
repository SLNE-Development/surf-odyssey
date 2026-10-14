package dev.slne.surf.odyssey.runtime.microservice.db.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable

/** The experience of every player per skill, one row per player and skill. */
object SkillTable : AuditableTable("odyssey_skills") {
    val ownerUuid = nativeUuid("owner_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)
    val skill = varchar("skill", 32)
    val xp = long("xp")

    override val primaryKey = PrimaryKey(ownerUuid, skill)
}
