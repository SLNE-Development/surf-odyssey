package dev.slne.surf.odyssey.runtime.microservice.db.table.progression

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable

/** The titles each player has unlocked, one row per player and title; at most one row per player is active. */
object TitleTable : AuditableTable("odyssey_titles") {
    /** The player who unlocked the title. */
    val player = nativeUuid("player_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)

    /** The id of the title definition. */
    val titleId = varchar("title_id", 64)

    /** Whether the player currently shows this title. */
    val active = bool("active").default(false)

    override val primaryKey = PrimaryKey(player, titleId)
}
