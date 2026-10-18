package dev.slne.surf.odyssey.runtime.microservice.db.table.progression

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable

/** Recent reports of which player held which tool, used to detect duplicated tool items. */
object ToolSightingTable : AuditableTable("odyssey_tool_sightings") {
    /** The row id. */
    val id = long("id").autoIncrement()

    /** The tool that was seen. */
    val tool = nativeUuid("tool_uuid")
        .references(ToolTable.uuid, onDelete = ReferenceOption.CASCADE)
        .index()

    /** The player who held the tool. */
    val holder = nativeUuid("holder_uuid")

    /** The server the tool was seen on. */
    val server = varchar("server", 64)

    /** The epoch milliseconds the tool was seen at. */
    val seenAt = long("seen_at").index()

    override val primaryKey = PrimaryKey(id)
}
