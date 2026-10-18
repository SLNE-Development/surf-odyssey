package dev.slne.surf.odyssey.runtime.microservice.db.table.progression

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable

/** The quest each player tracks; players without a tracked quest have no row. */
object TrackedQuestTable : AuditableTable("odyssey_tracked_quests") {
    /** The player who tracks the quest. */
    val player = nativeUuid("player_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)

    /** The id of the tracked quest definition. */
    val questId = varchar("quest_id", 64)

    override val primaryKey = PrimaryKey(player)
}
