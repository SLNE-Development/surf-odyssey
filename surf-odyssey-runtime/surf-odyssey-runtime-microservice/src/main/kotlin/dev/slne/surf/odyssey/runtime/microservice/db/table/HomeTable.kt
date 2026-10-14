package dev.slne.surf.odyssey.runtime.microservice.db.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable

/** The homes of all players, keyed by the id the client assigns to each home. */
object HomeTable : AuditableTable("odyssey_homes") {
    val id = nativeUuid("id")
    val ownerUuid = nativeUuid("owner_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)
        .index()
    val worldUuid = nativeUuid("world_uuid")
    val name = varchar("name", 16)
    val description = varchar("description", 500)
    val itemType = varchar("item_type", 128)
    val x = double("x")
    val y = double("y")
    val z = double("z")
    val yaw = float("yaw")
    val pitch = float("pitch")

    override val primaryKey = PrimaryKey(id)
}
