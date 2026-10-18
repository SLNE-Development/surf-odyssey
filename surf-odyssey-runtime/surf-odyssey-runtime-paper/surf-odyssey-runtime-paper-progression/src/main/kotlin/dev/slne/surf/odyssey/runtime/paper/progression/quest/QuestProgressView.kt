package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.core.common.progression.CounterDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import java.time.Instant

/**
 * The progress of one active quest.
 *
 * @property step the index of the current step
 * @property counts the count of each objective of the current step, capped at the objective's amount
 * @property complete whether every objective of the current step is met
 */
data class QuestProgress(val step: Int, val counts: List<Int>, val complete: Boolean)

/**
 * A quest a player is currently working on.
 *
 * @property quest the quest definition
 * @property step the index of the current step
 * @property period the period index the quest's counters belong to
 */
data class ActiveQuest(val quest: QuestDefinition, val step: Int, val period: Long)

/** Computes quest progress from stored quest states and counters. */
object QuestProgressView {
    /**
     * Returns the period index [quest] counts in at [now].
     *
     * @return the current period of the quest's type for recurring quests, `0` for the others
     */
    fun periodOf(quest: QuestDefinition, now: Instant): Long =
        if (quest.type.recurring) Periods.index(quest.type, now) else 0L

    /**
     * Returns whether [state] is an active state of [quest] that still counts: its period is [period] for recurring
     * quests, and its step exists in the definition.
     *
     * @param quest the quest definition
     * @param state the stored state, or `null`
     * @param period the current period of the quest's type
     */
    fun isCurrent(quest: QuestDefinition, state: QuestStateDto?, period: Long): Boolean =
        state != null && state.status == QuestStatus.ACTIVE &&
                (!quest.type.recurring || state.period == period) && state.step in quest.steps.indices

    /**
     * Computes the progress of [quest] from [state] and [counters].
     *
     * Only counters of the current step whose step version equals the version the definition gives that step and
     * whose period equals the state's period count. Each count is capped at its objective's amount.
     *
     * @param quest the quest definition
     * @param state the stored state of the quest, or `null`
     * @param counters the stored counters of the player
     * @param period the current period of the quest's type, see [periodOf]
     * @return the progress, or `null` if the quest is not active, is recurring and belongs to another period, or is
     *         on a step the definition does not have
     */
    fun compute(quest: QuestDefinition, state: QuestStateDto?, counters: List<CounterDto>, period: Long): QuestProgress? {
        if (!isCurrent(quest, state, period)) return null
        state!!
        val step = quest.steps[state.step]
        val counts = step.objectives.mapIndexed { index, objective ->
            counters.asSequence()
                .filter {
                    it.questId == quest.id && it.step == state.step && it.stepVersion == step.version &&
                            it.objective == index && it.period == state.period
                }
                .sumOf { it.value }
                .coerceIn(0, objective.amount)
        }
        val complete = counts.withIndex().all { (index, count) -> count >= step.objectives[index].amount }
        return QuestProgress(state.step, counts, complete)
    }

    /**
     * Lists the quests of [state] that are active and still count at [now], in the order of the stored states.
     * States of quests the registry does not know are skipped.
     *
     * @param registry the installed definitions
     * @param state the player's stored progression
     * @param now the current time
     */
    fun activeQuests(registry: ProgressionRegistry, state: ProgressionStateDto, now: Instant): List<ActiveQuest> =
        state.quests.mapNotNull { stored ->
            val quest = registry.quest(stored.questId) ?: return@mapNotNull null
            if (!isCurrent(quest, stored, periodOf(quest, now))) return@mapNotNull null
            ActiveQuest(quest, stored.step, stored.period)
        }

    /**
     * Returns whether the tracked quest of [state] is a known quest that is active and still counts at [now].
     *
     * @param registry the installed definitions
     * @param state the player's stored progression
     * @param now the current time
     * @return `false` if no quest is tracked, or the tracked quest is unknown, not active or of an earlier period
     */
    fun tracksActiveQuest(registry: ProgressionRegistry, state: ProgressionStateDto, now: Instant): Boolean {
        val quest = state.trackedQuest?.let(registry::quest) ?: return false
        return isCurrent(quest, state.quests.find { it.questId == quest.id }, periodOf(quest, now))
    }

    /**
     * Picks the quest to track next: the first active main quest, otherwise the first active quest in the order of
     * the stored states.
     *
     * @param registry the installed definitions
     * @param state the player's stored progression
     * @param now the current time
     * @return the quest, or `null` if no quest is active
     */
    fun nextTracked(registry: ProgressionRegistry, state: ProgressionStateDto, now: Instant): QuestDefinition? {
        val active = activeQuests(registry, state, now)
        return (active.firstOrNull { it.quest.type == QuestType.MAIN } ?: active.firstOrNull())?.quest
    }
}
