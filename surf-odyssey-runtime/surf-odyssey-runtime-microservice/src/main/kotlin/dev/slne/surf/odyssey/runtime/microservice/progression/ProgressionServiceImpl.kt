package dev.slne.surf.odyssey.runtime.microservice.progression

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ResultRow
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.SortOrder
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.greaterEq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.inList
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.isNull
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.less
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.plus
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.statements.insertValue
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.deleteWhere
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertIgnore
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.select
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.update
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.upsert
import dev.slne.surf.odyssey.core.common.progression.CounterDeltaDto
import dev.slne.surf.odyssey.core.common.progression.CounterDto
import dev.slne.surf.odyssey.core.common.progression.DeliveryDto
import dev.slne.surf.odyssey.core.common.progression.OperationDto
import dev.slne.surf.odyssey.core.common.progression.OperationResultDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionService
import dev.slne.surf.odyssey.core.common.progression.ProgressionStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.core.common.progression.StationJobDto
import dev.slne.surf.odyssey.core.common.progression.ToolRecordDto
import dev.slne.surf.odyssey.core.common.progression.ToolSightingResult
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.DeliveryTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.OperationOutcome
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.OperationTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.QuestCounterTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.QuestStateTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.StationJobTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.TitleTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.ToolSightingTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.ToolTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.TrackedQuestTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.UnlockedRecipeTable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.time.Clock
import java.time.OffsetDateTime
import java.util.*

/**
 * Stores the progression of all players in the database.
 *
 * Every call that changes a player's state first locks that player's row, so concurrent calls for the same player
 * from several servers run one after another.
 *
 * @param clock the clock that supplies the time for every stored timestamp
 */
class ProgressionServiceImpl(private val clock: Clock) : ProgressionService {
    /** Reads the quests, counters, unlocks, open deliveries and open jobs of [player]. */
    override suspend fun loadState(player: SerializableUUID): ProgressionStateDto = suspendTransaction {
        selectState(player)
    }

    /** Adds each delta to its stored counter in one transaction, creating missing counters with the delta as value. */
    override suspend fun applyCounterDeltas(player: SerializableUUID, deltas: List<CounterDeltaDto>) {
        if (deltas.isEmpty()) return
        suspendTransaction {
            lockPlayer(player)
            for (delta in deltas) {
                QuestCounterTable.upsert(
                    onUpdate = {
                        it[QuestCounterTable.value] = QuestCounterTable.value + insertValue(QuestCounterTable.value)
                    }
                ) {
                    it[QuestCounterTable.player] = player
                    it[questId] = delta.questId
                    it[step] = delta.step
                    it[stepVersion] = delta.stepVersion
                    it[objective] = delta.objective
                    it[period] = delta.period
                    it[value] = delta.delta
                }
            }
        }
    }

    /**
     * Returns the stored result for a known operation id. Otherwise checks the operation against the current state
     * and stores either all of its changes and deliveries or only the rejection.
     */
    override suspend fun commitOperation(operation: OperationDto): OperationResultDto = suspendTransaction {
        lockPlayer(operation.player)
        storedResult(operation.id)?.let { return@suspendTransaction it }

        val knownJobIds = existingJobIds(operation.changes.filterIsInstance<StateChange.JobStarted>().map { it.job.id })
        when (val decision = OperationApplier.decide(selectState(operation.player), operation, knownJobIds)) {
            is Decision.Reject -> {
                insertOperation(operation, OperationOutcome.REJECTED, decision.reason)
                OperationResultDto.Rejected(decision.reason)
            }

            Decision.Apply -> {
                val now = clock.millis()
                operation.changes.forEach { writeChange(operation.player, it, now) }
                val deliveryIds = operation.deliveries.mapIndexed { position, delivery ->
                    val id = UUID.randomUUID()
                    DeliveryTable.insert {
                        it[DeliveryTable.id] = id
                        it[player] = operation.player
                        it[operationId] = operation.id
                        it[DeliveryTable.position] = position
                        it[item] = delivery.itemBase64
                        it[deliverySource] = delivery.source
                        it[createdAt] = OffsetDateTime.now(clock)
                    }
                    id
                }
                insertOperation(operation, OperationOutcome.APPLIED, null)
                OperationResultDto.Applied(deliveryIds)
            }
        }
    }

    /** Marks the delivery as claimed only while it is open and returns its item to the call whose update succeeded. */
    override suspend fun claimDelivery(player: SerializableUUID, deliveryId: SerializableUUID): String? =
        suspendTransaction {
            val claimed = DeliveryTable.update({
                (DeliveryTable.id eq deliveryId) and (DeliveryTable.player eq player) and
                        DeliveryTable.claimedAt.isNull()
            }) {
                it[claimedAt] = clock.millis()
            }
            if (claimed == 0) return@suspendTransaction null

            DeliveryTable.select(DeliveryTable.item)
                .where { DeliveryTable.id eq deliveryId }
                .firstOrNull()
                ?.get(DeliveryTable.item)
        }

    /** Marks an active quest as abandoned and deletes its counters. */
    override suspend fun abandonQuest(player: SerializableUUID, questId: String): Boolean = suspendTransaction {
        lockPlayer(player)
        val abandoned = QuestStateTable.update({
            (QuestStateTable.player eq player) and (QuestStateTable.questId eq questId) and
                    (QuestStateTable.status eq QuestStatus.ACTIVE)
        }) {
            it[status] = QuestStatus.ABANDONED
        }
        if (abandoned == 0) return@suspendTransaction false

        deleteCounters(player, questId)
        true
    }

    /** Stores or deletes the tracked quest row of [player]. */
    override suspend fun setTrackedQuest(player: SerializableUUID, questId: String?): Unit = suspendTransaction {
        lockPlayer(player)
        if (questId == null) {
            TrackedQuestTable.deleteWhere { TrackedQuestTable.player eq player }
        } else {
            TrackedQuestTable.upsert {
                it[TrackedQuestTable.player] = player
                it[TrackedQuestTable.questId] = questId
            }
        }
    }

    /** Makes [titleId] the only active title of [player], or deactivates all titles when it is `null`. */
    override suspend fun setActiveTitle(player: SerializableUUID, titleId: String?): Boolean = suspendTransaction {
        lockPlayer(player)
        if (titleId != null) {
            val unlocked = TitleTable.select(TitleTable.titleId)
                .where { (TitleTable.player eq player) and (TitleTable.titleId eq titleId) }
                .firstOrNull() != null
            if (!unlocked) return@suspendTransaction false
        }

        TitleTable.update({ (TitleTable.player eq player) and (TitleTable.active eq true) }) {
            it[active] = false
        }
        if (titleId != null) {
            TitleTable.update({ (TitleTable.player eq player) and (TitleTable.titleId eq titleId) }) {
                it[active] = true
            }
        }
        true
    }

    /** Inserts the tool registration unless a tool with the same id exists. */
    override suspend fun registerTool(tool: SerializableUUID, toolId: String, owner: SerializableUUID): Unit =
        suspendTransaction {
            ToolTable.insertIgnore {
                it[uuid] = tool
                it[ToolTable.toolId] = toolId
                it[creator] = owner
            }
        }

    /**
     * Stores the sighting, deletes sightings older than the retention time, updates the last holder and checks the
     * recent sightings of the tool with [DuplicateRule].
     */
    override suspend fun reportToolSighting(
        tool: SerializableUUID,
        holder: SerializableUUID,
        server: String
    ): ToolSightingResult = suspendTransaction {
        val known = ToolTable.select(ToolTable.uuid).where { ToolTable.uuid eq tool }.firstOrNull() != null
        if (!known) return@suspendTransaction ToolSightingResult.UNKNOWN_TOOL

        val now = clock.millis()
        ToolSightingTable.deleteWhere { seenAt less now - DuplicateRule.RETENTION_MILLIS }
        ToolSightingTable.insert {
            it[ToolSightingTable.tool] = tool
            it[ToolSightingTable.holder] = holder
            it[ToolSightingTable.server] = server
            it[seenAt] = now
        }
        ToolTable.update({ ToolTable.uuid eq tool }) {
            it[lastHolder] = holder
            it[lastSeenAt] = now
        }

        val sightings = ToolSightingTable.select(ToolSightingTable.holder, ToolSightingTable.seenAt)
            .where {
                (ToolSightingTable.tool eq tool) and
                        (ToolSightingTable.seenAt greaterEq now - DuplicateRule.WINDOW_MILLIS)
            }
            .map { Sighting(it[ToolSightingTable.holder], it[ToolSightingTable.seenAt]) }
            .toList()

        if (DuplicateRule.isSuspected(sightings, now)) {
            ToolSightingResult.DUPLICATE_SUSPECTED
        } else {
            ToolSightingResult.OK
        }
    }

    /** Reads the registration of [tool]. */
    override suspend fun lookupTool(tool: SerializableUUID): ToolRecordDto? = suspendTransaction {
        ToolTable.selectAll()
            .where { ToolTable.uuid eq tool }
            .firstOrNull()
            ?.let {
                ToolRecordDto(
                    tool = it[ToolTable.uuid],
                    toolId = it[ToolTable.toolId],
                    creator = it[ToolTable.creator],
                    lastHolder = it[ToolTable.lastHolder],
                    lastSeenAt = it[ToolTable.lastSeenAt]
                )
            }
    }

    /** Deletes the quest state and all counters of the quest. */
    override suspend fun resetQuest(player: SerializableUUID, questId: String): Unit = suspendTransaction {
        lockPlayer(player)
        QuestStateTable.deleteWhere { (QuestStateTable.player eq player) and (QuestStateTable.questId eq questId) }
        deleteCounters(player, questId)
    }

    /** Deletes the unlocked recipe row. */
    override suspend fun revokeRecipe(player: SerializableUUID, recipeId: String): Unit = suspendTransaction {
        lockPlayer(player)
        UnlockedRecipeTable.deleteWhere {
            (UnlockedRecipeTable.player eq player) and (UnlockedRecipeTable.recipeId eq recipeId)
        }
    }

    /** Deletes the title row, which also clears it when it is active. */
    override suspend fun revokeTitle(player: SerializableUUID, titleId: String): Unit = suspendTransaction {
        lockPlayer(player)
        TitleTable.deleteWhere { (TitleTable.player eq player) and (TitleTable.titleId eq titleId) }
    }

    /**
     * Creates the player row if needed and locks it until the current transaction ends, so that no other call can
     * change the player's progression in the meantime. Must run inside a transaction.
     */
    private suspend fun lockPlayer(player: UUID) {
        OdysseyPlayerTable.insertIgnore { it[uuid] = player }
        OdysseyPlayerTable.select(OdysseyPlayerTable.uuid)
            .where { OdysseyPlayerTable.uuid eq player }
            .forUpdate()
            .firstOrNull()
    }

    /**
     * Returns the result of an operation that was already committed with [operationId], or `null` if there was none.
     * Must run inside a transaction.
     */
    private suspend fun storedResult(operationId: UUID): OperationResultDto? {
        val row = OperationTable.selectAll()
            .where { OperationTable.id eq operationId }
            .firstOrNull()
            ?: return null

        return when (row[OperationTable.result]) {
            OperationOutcome.REJECTED -> OperationResultDto.Rejected(row[OperationTable.reason].orEmpty())
            OperationOutcome.APPLIED -> OperationResultDto.AlreadyApplied(
                DeliveryTable.select(DeliveryTable.id)
                    .where { DeliveryTable.operationId eq operationId }
                    .orderBy(DeliveryTable.position, SortOrder.ASC)
                    .map { it[DeliveryTable.id] }
                    .toList()
            )
        }
    }

    /**
     * Returns which of [jobIds] are already stored, for any player and whether collected or not. Must run inside a
     * transaction.
     */
    private suspend fun existingJobIds(jobIds: List<UUID>): Set<UUID> {
        if (jobIds.isEmpty()) return emptySet()
        return StationJobTable.select(StationJobTable.id)
            .where { StationJobTable.id inList jobIds }
            .map { it[StationJobTable.id] }
            .toList()
            .toSet()
    }

    /** Records that [operation] was committed with [outcome]. Must run inside a transaction. */
    private suspend fun insertOperation(operation: OperationDto, outcome: OperationOutcome, reason: String?) {
        OperationTable.insert {
            it[id] = operation.id
            it[player] = operation.player
            it[result] = outcome
            it[OperationTable.reason] = reason
            it[createdAt] = OffsetDateTime.now(clock)
        }
    }

    /** Writes one change of an applied operation for [player] at the epoch milliseconds [now]. */
    private suspend fun writeChange(player: UUID, change: StateChange, now: Long) {
        when (change) {
            is StateChange.QuestAccepted -> {
                QuestStateTable.upsert {
                    it[QuestStateTable.player] = player
                    it[questId] = change.questId
                    it[status] = QuestStatus.ACTIVE
                    it[step] = 0
                    it[stepVersion] = change.stepVersion
                    it[period] = change.period
                    it[completedAt] = null
                }
                deleteCounters(player, change.questId)
            }

            is StateChange.QuestStepAdvanced -> QuestStateTable.update({ questRow(player, change.questId) }) {
                it[step] = change.toStep
                it[stepVersion] = change.stepVersion
            }

            is StateChange.QuestCompleted -> QuestStateTable.update({ questRow(player, change.questId) }) {
                it[status] = QuestStatus.COMPLETED
                it[completedAt] = now
            }

            is StateChange.CounterSet -> QuestCounterTable.upsert {
                it[QuestCounterTable.player] = player
                it[questId] = change.questId
                it[step] = change.step
                it[stepVersion] = change.stepVersion
                it[objective] = change.objective
                it[period] = change.period
                it[value] = change.value
            }

            is StateChange.RecipeUnlocked -> UnlockedRecipeTable.insertIgnore {
                it[UnlockedRecipeTable.player] = player
                it[recipeId] = change.recipeId
            }

            is StateChange.TitleUnlocked -> TitleTable.insertIgnore {
                it[TitleTable.player] = player
                it[titleId] = change.titleId
            }

            is StateChange.JobStarted -> StationJobTable.insert {
                it[id] = change.job.id
                it[StationJobTable.player] = player
                it[stationType] = change.job.stationType
                it[recipeId] = change.job.recipeId
                it[amount] = change.job.amount
                it[startedAt] = change.job.startedAt
                it[readyAt] = change.job.readyAt
            }

            is StateChange.JobCollected -> StationJobTable.update({
                (StationJobTable.id eq change.jobId) and (StationJobTable.player eq player) and
                        StationJobTable.collectedAt.isNull()
            }) {
                it[collectedAt] = now
            }
        }
    }

    /** Matches the quest state row of [questId] for [player]. */
    private fun questRow(player: UUID, questId: String) =
        (QuestStateTable.player eq player) and (QuestStateTable.questId eq questId)

    /** Deletes every counter of [questId] for [player]. Must run inside a transaction. */
    private suspend fun deleteCounters(player: UUID, questId: String) {
        QuestCounterTable.deleteWhere {
            (QuestCounterTable.player eq player) and (QuestCounterTable.questId eq questId)
        }
    }

    /** Reads the whole progression state of [player]. Must run inside a transaction. */
    private suspend fun selectState(player: UUID): ProgressionStateDto {
        val quests = QuestStateTable.selectAll()
            .where { QuestStateTable.player eq player }
            .map { it.toQuestState() }
            .toList()
        val counters = QuestCounterTable.selectAll()
            .where { QuestCounterTable.player eq player }
            .map { it.toCounter() }
            .toList()
        val recipes = UnlockedRecipeTable.select(UnlockedRecipeTable.recipeId)
            .where { UnlockedRecipeTable.player eq player }
            .map { it[UnlockedRecipeTable.recipeId] }
            .toList()
            .toSet()
        val titleRows = TitleTable.select(TitleTable.titleId, TitleTable.active)
            .where { TitleTable.player eq player }
            .map { it[TitleTable.titleId] to it[TitleTable.active] }
            .toList()
        val trackedQuest = TrackedQuestTable.select(TrackedQuestTable.questId)
            .where { TrackedQuestTable.player eq player }
            .firstOrNull()
            ?.get(TrackedQuestTable.questId)
        val deliveries = DeliveryTable.selectAll()
            .where { (DeliveryTable.player eq player) and DeliveryTable.claimedAt.isNull() }
            .orderBy(DeliveryTable.createdAt to SortOrder.ASC, DeliveryTable.position to SortOrder.ASC)
            .map { it.toDelivery() }
            .toList()
        val jobs = StationJobTable.selectAll()
            .where { (StationJobTable.player eq player) and StationJobTable.collectedAt.isNull() }
            .orderBy(StationJobTable.startedAt, SortOrder.ASC)
            .map { it.toJob() }
            .toList()

        return ProgressionStateDto(
            player = player,
            quests = quests,
            counters = counters,
            recipes = recipes,
            titles = titleRows.mapTo(mutableSetOf()) { it.first },
            activeTitle = titleRows.firstOrNull { it.second }?.first,
            trackedQuest = trackedQuest,
            deliveries = deliveries,
            jobs = jobs
        )
    }

    /** Converts a quest state row. */
    private fun ResultRow.toQuestState() = QuestStateDto(
        questId = this[QuestStateTable.questId],
        status = this[QuestStateTable.status],
        step = this[QuestStateTable.step],
        stepVersion = this[QuestStateTable.stepVersion],
        period = this[QuestStateTable.period],
        completedAt = this[QuestStateTable.completedAt]
    )

    /** Converts a counter row. */
    private fun ResultRow.toCounter() = CounterDto(
        questId = this[QuestCounterTable.questId],
        step = this[QuestCounterTable.step],
        stepVersion = this[QuestCounterTable.stepVersion],
        objective = this[QuestCounterTable.objective],
        period = this[QuestCounterTable.period],
        value = this[QuestCounterTable.value]
    )

    /** Converts a delivery row. */
    private fun ResultRow.toDelivery() = DeliveryDto(
        id = this[DeliveryTable.id],
        itemBase64 = this[DeliveryTable.item],
        source = this[DeliveryTable.deliverySource],
        createdAt = this[DeliveryTable.createdAt].toInstant().toEpochMilli()
    )

    /** Converts a station job row. */
    private fun ResultRow.toJob() = StationJobDto(
        id = this[StationJobTable.id],
        stationType = this[StationJobTable.stationType],
        recipeId = this[StationJobTable.recipeId],
        amount = this[StationJobTable.amount],
        startedAt = this[StationJobTable.startedAt],
        readyAt = this[StationJobTable.readyAt]
    )
}
