package dev.slne.surf.odyssey.runtime.microservice.db.table.progression

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable
import dev.slne.surf.odyssey.runtime.microservice.progression.ProgressionLimits

/** The timed station jobs of all players. A job is open until [collectedAt] is set. */
object StationJobTable : AuditableTable("odyssey_station_jobs") {
    /** The id of the job. */
    val id = nativeUuid("id")

    /** The player who started the job. */
    val player = nativeUuid("player_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)
        .index()

    /** The station type the job runs at. */
    val stationType = varchar("station_type", ProgressionLimits.MAX_ID_LENGTH)

    /** The id of the recipe being crafted. */
    val recipeId = varchar("recipe_id", ProgressionLimits.MAX_ID_LENGTH)

    /** How many times the recipe is crafted. */
    val amount = integer("amount")

    /** The epoch milliseconds the job started at. */
    val startedAt = long("started_at")

    /** The epoch milliseconds from which the job can be collected. */
    val readyAt = long("ready_at")

    /** The epoch milliseconds the job was collected at, or `null` while it is open. */
    val collectedAt = long("collected_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
