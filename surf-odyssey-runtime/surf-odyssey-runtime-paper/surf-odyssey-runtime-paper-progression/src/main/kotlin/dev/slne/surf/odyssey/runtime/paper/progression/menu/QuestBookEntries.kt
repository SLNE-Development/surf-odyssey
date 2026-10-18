package dev.slne.surf.odyssey.runtime.paper.progression.menu

import dev.slne.surf.odyssey.core.common.progression.ProgressionStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import dev.slne.surf.odyssey.runtime.paper.progression.quest.Periods
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestProgress
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestProgressView
import dev.slne.surf.odyssey.runtime.paper.progression.quest.RecurringDraw
import java.time.Duration
import java.time.Instant

/**
 * An active quest in the quest book.
 *
 * @property quest the quest
 * @property progress the progress of its current step
 * @property tracked whether the player tracks the quest
 * @property abandonable whether the player may abandon the quest
 */
data class ActiveQuestEntry(
    val quest: QuestDefinition,
    val progress: QuestProgress,
    val tracked: Boolean,
    val abandonable: Boolean,
)

/** Where a drawn recurring quest stands in the current period. */
enum class RecurringStatus {
    /** Not accepted in the current period, or abandoned. */
    OPEN,

    /** Accepted and in progress. */
    ACTIVE,

    /** Completed in the current period. */
    COMPLETED,
}

/**
 * A recurring quest drawn for the player in the current period.
 *
 * @property quest the quest
 * @property status where the quest stands in the period
 * @property remaining the time until the period ends
 */
data class RecurringQuestEntry(val quest: QuestDefinition, val status: RecurringStatus, val remaining: Duration)

/**
 * The sections of the quest book.
 *
 * @property active the active quests in the order of the stored states
 * @property recurring the drawn recurring quests of the current periods, daily first, then weekly and monthly
 * @property completedCount how many quests the player completed
 * @property recentlyCompleted the [RECENT_COMPLETED] most recently completed quests, newest first
 */
data class QuestBookSections(
    val active: List<ActiveQuestEntry>,
    val recurring: List<RecurringQuestEntry>,
    val completedCount: Int,
    val recentlyCompleted: List<QuestDefinition>,
) {
    /** Limits of the sections. */
    companion object {
        /** How many completed quests the book lists. */
        const val RECENT_COMPLETED = 10
    }
}

/** Builds the sections of the quest book from the definitions, the player's progression and the time. */
object QuestBookEntries {
    /** The recurring quest types in the order the book lists them. */
    private val RECURRING_TYPES = listOf(QuestType.DAILY, QuestType.WEEKLY, QuestType.MONTHLY)

    /**
     * Builds the quest book of the player whose progression is [state].
     *
     * Abandoned quests, quests the registry does not know and recurring quests of an earlier period are not listed
     * as active or completed. Main quests are never abandonable.
     *
     * @param registry the installed definitions
     * @param state the player's progression
     * @param now the current time
     */
    fun build(registry: ProgressionRegistry, state: ProgressionStateDto, now: Instant): QuestBookSections {
        val active = QuestProgressView.activeQuests(registry, state, now).mapNotNull { active ->
            val stored = state.quests.find { it.questId == active.quest.id }
            val progress = QuestProgressView.compute(active.quest, stored, state.counters, active.period)
                ?: return@mapNotNull null
            ActiveQuestEntry(
                quest = active.quest,
                progress = progress,
                tracked = state.trackedQuest == active.quest.id,
                abandonable = active.quest.type.abandonable,
            )
        }
        val completed = state.quests
            .filter { it.status == QuestStatus.COMPLETED }
            .mapNotNull { stored -> registry.quest(stored.questId)?.let { it to (stored.completedAt ?: 0L) } }
        return QuestBookSections(
            active = active,
            recurring = recurring(registry, state, now),
            completedCount = completed.size,
            recentlyCompleted = completed.sortedByDescending { it.second }
                .take(QuestBookSections.RECENT_COMPLETED)
                .map { it.first },
        )
    }

    /** Lists the quests drawn for the player of [state] in the current periods with their status and remaining time. */
    private fun recurring(registry: ProgressionRegistry, state: ProgressionStateDto, now: Instant) =
        RECURRING_TYPES.flatMap { type ->
            val period = Periods.index(type, now)
            val remaining = Duration.between(now, Periods.endOf(type, period))
            RecurringDraw.draw(state.player, type, period, registry.pool(type), registry.pools.countFor(type))
                .map { quest ->
                    val stored = state.quests.find { it.questId == quest.id }?.takeIf { it.period == period }
                    val status = when (stored?.status) {
                        QuestStatus.ACTIVE -> RecurringStatus.ACTIVE
                        QuestStatus.COMPLETED -> RecurringStatus.COMPLETED
                        QuestStatus.ABANDONED, null -> RecurringStatus.OPEN
                    }
                    RecurringQuestEntry(quest, status, remaining)
                }
        }
}
