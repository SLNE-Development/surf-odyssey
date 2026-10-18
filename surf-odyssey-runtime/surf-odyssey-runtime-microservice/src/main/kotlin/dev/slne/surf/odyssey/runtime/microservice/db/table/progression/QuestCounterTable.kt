package dev.slne.surf.odyssey.runtime.microservice.db.table.progression

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable
import dev.slne.surf.odyssey.runtime.microservice.progression.ProgressionLimits

/** The objective counters of the players' quests, one row per objective of a step version and period. */
object QuestCounterTable : AuditableTable("odyssey_quest_counters") {
    /** The player the counter belongs to. */
    val player = nativeUuid("player_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)

    /** The id of the quest definition. */
    val questId = varchar("quest_id", ProgressionLimits.MAX_ID_LENGTH)

    /** The index of the step the objective belongs to. */
    val step = integer("step")

    /** The version of the step the counter was recorded against. */
    val stepVersion = integer("step_version")

    /** The index of the objective within the step. */
    val objective = integer("objective")

    /** The period index the counter belongs to. */
    val period = long("period")

    /** The current count. */
    val value = integer("value")

    override val primaryKey = PrimaryKey(player, questId, step, stepVersion, objective, period)
}
