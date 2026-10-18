package dev.slne.surf.odyssey.runtime.microservice.db.table.progression

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.table.AuditableTable
import dev.slne.surf.odyssey.runtime.microservice.progression.ProgressionLimits

/** Every registered progression tool item, keyed by the unique id stored on the item. */
object ToolTable : AuditableTable("odyssey_tools") {
    /** The unique id stored on the item. */
    val uuid = nativeUuid("uuid")

    /** The id of the tool definition. */
    val toolId = varchar("tool_id", ProgressionLimits.MAX_ID_LENGTH)

    /** The player the tool was created for. */
    val creator = nativeUuid("creator_uuid")

    /** The player the tool was last seen with, or `null` if it was never seen. */
    val lastHolder = nativeUuid("last_holder_uuid").nullable()

    /** The epoch milliseconds the tool was last seen at, or `null` if it was never seen. */
    val lastSeenAt = long("last_seen_at").nullable()

    override val primaryKey = PrimaryKey(uuid)
}
