package dev.slne.surf.odyssey.runtime.microservice.db.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.table.AuditableTable

/** Every player that has joined an odyssey server at least once. */
object OdysseyPlayerTable : AuditableTable("odyssey_players") {
    val uuid = nativeUuid("uuid")

    override val primaryKey = PrimaryKey(uuid)
}
