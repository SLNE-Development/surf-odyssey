package dev.slne.surf.odyssey.runtime.microservice.db.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable

/** Whether each player sees the scoreboard; players without a row see it. */
object ScoreboardSettingTable : AuditableTable("odyssey_scoreboard_settings") {
    val ownerUuid = nativeUuid("owner_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)
    val enabled = bool("enabled").default(true)

    override val primaryKey = PrimaryKey(ownerUuid)
}
