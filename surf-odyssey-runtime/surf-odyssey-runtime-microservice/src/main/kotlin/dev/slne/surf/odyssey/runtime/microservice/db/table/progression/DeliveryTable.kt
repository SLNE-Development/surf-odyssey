package dev.slne.surf.odyssey.runtime.microservice.db.table.progression

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable
import dev.slne.surf.odyssey.runtime.microservice.progression.ProgressionLimits

/**
 * The items in the players' pickup boxes. A delivery is open until [claimedAt] is set; [AuditableTable.createdAt] is
 * the time the delivery was created.
 */
object DeliveryTable : AuditableTable("odyssey_deliveries") {
    /** The id of the delivery. */
    val id = nativeUuid("id")

    /** The player the delivery belongs to. */
    val player = nativeUuid("player_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)
        .index()

    /** The operation that created the delivery. */
    val operationId = nativeUuid("operation_id").index()

    /** The position of the delivery within the deliveries of its operation. */
    val position = integer("position")

    /** The item serialized as bytes and encoded as Base64. */
    val item = text("item")

    /** A short identifier of what created the delivery. */
    val deliverySource = varchar("source", ProgressionLimits.MAX_ID_LENGTH)

    /** The epoch milliseconds the delivery was claimed at, or `null` while it is open. */
    val claimedAt = long("claimed_at").nullable()

    /** Identifies a delivery by its [id]. */
    override val primaryKey = PrimaryKey(id)
}
