package dev.slne.surf.odyssey.runtime.microservice.db.table.progression

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable

/**
 * Every committed operation and its outcome, so a repeated commit returns the same outcome.
 * [AuditableTable.createdAt] is the time the operation was committed.
 */
object OperationTable : AuditableTable("odyssey_operations") {
    /** The id of the operation. */
    val id = nativeUuid("id")

    /** The player the operation belongs to. */
    val player = nativeUuid("player_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)
        .index()

    /** Whether the operation was applied or rejected. */
    val result = enumerationByName("result", 16, OperationOutcome::class)

    /** Why the operation was rejected, or `null` if it was applied. */
    val reason = varchar("reason", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

/** The stored outcome of an operation. */
enum class OperationOutcome {
    /** All changes and deliveries of the operation were stored. */
    APPLIED,

    /** Nothing of the operation was stored. */
    REJECTED
}
