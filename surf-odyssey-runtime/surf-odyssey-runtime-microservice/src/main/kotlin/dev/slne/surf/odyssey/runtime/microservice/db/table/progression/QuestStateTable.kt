package dev.slne.surf.odyssey.runtime.microservice.db.table.progression

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable
import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable

/** The state of every quest each player has accepted, one row per player and quest. */
object QuestStateTable : AuditableTable("odyssey_quest_states") {
    /** The player the quest state belongs to. */
    val player = nativeUuid("player_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)

    /** The id of the quest definition. */
    val questId = varchar("quest_id", 64)

    /** Where the quest stands in its lifecycle. */
    val status = enumerationByName("status", 16, QuestStatus::class)

    /** The index of the step the player is on. */
    val step = integer("step")

    /** The version of the step the progress was recorded against. */
    val stepVersion = integer("step_version")

    /** The period index the quest was accepted in. */
    val period = long("period")

    /** The epoch milliseconds the quest was completed at, or `null` while not completed. */
    val completedAt = long("completed_at").nullable()

    override val primaryKey = PrimaryKey(player, questId)
}
