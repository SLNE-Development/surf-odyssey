package dev.slne.surf.odyssey.runtime.microservice.progression

import dev.slne.surf.odyssey.core.common.progression.OperationDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.core.common.progression.StateChange

/** Whether an operation may be stored. */
sealed interface Decision {
    /** Every change of the operation can be applied, so the whole operation is stored. */
    data object Apply : Decision

    /**
     * At least one change cannot be applied, so nothing of the operation is stored.
     *
     * @property reason a short description of the first change that cannot be applied
     */
    data class Reject(val reason: String) : Decision
}

/** Checks the changes of an operation against a player's current state without touching the database. */
object OperationApplier {
    /**
     * Decides whether all changes of [op] can be applied to [current].
     *
     * The changes are checked in order, and each one is checked against the state the earlier changes produce, so an
     * operation may accept a quest and advance it in one go. A single change that cannot be applied rejects the whole
     * operation.
     */
    fun decide(current: ProgressionStateDto, op: OperationDto): Decision {
        var state = current
        for (change in op.changes) {
            state = when (val result = check(state, change)) {
                is Checked.Ok -> result.state
                is Checked.Failed -> return Decision.Reject(result.reason)
            }
        }
        return Decision.Apply
    }

    /** The result of checking one change: the state after it, or why it cannot be applied. */
    private sealed interface Checked {
        /**
         * The change can be applied.
         *
         * @property state the state after the change
         */
        data class Ok(val state: ProgressionStateDto) : Checked

        /**
         * The change cannot be applied.
         *
         * @property reason a short description of why
         */
        data class Failed(val reason: String) : Checked
    }

    /** Checks [change] against [state] and returns the state after it, or why it cannot be applied. */
    private fun check(state: ProgressionStateDto, change: StateChange): Checked = when (change) {
        is StateChange.QuestAccepted -> acceptQuest(state, change)
        is StateChange.QuestStepAdvanced -> withActiveQuest(state, change.questId, change.fromStep) {
            it.copy(step = change.toStep, stepVersion = change.stepVersion)
        }
        is StateChange.QuestCompleted -> withActiveQuest(state, change.questId, change.fromStep) {
            it.copy(status = QuestStatus.COMPLETED)
        }
        is StateChange.CounterSet -> Checked.Ok(state)
        is StateChange.RecipeUnlocked -> Checked.Ok(state.copy(recipes = state.recipes + change.recipeId))
        is StateChange.TitleUnlocked -> Checked.Ok(state.copy(titles = state.titles + change.titleId))
        is StateChange.JobStarted ->
            if (state.jobs.any { it.id == change.job.id }) {
                Checked.Failed("job ${change.job.id} already exists")
            } else {
                Checked.Ok(state.copy(jobs = state.jobs + change.job))
            }
        is StateChange.JobCollected ->
            if (state.jobs.none { it.id == change.jobId }) {
                Checked.Failed("job ${change.jobId} does not exist or was already collected")
            } else {
                Checked.Ok(state.copy(jobs = state.jobs.filterNot { it.id == change.jobId }))
            }
    }

    /**
     * Accepts a quest unless it is active or completed in the same period. Any other state of the quest is replaced
     * and its counters are dropped.
     */
    private fun acceptQuest(state: ProgressionStateDto, change: StateChange.QuestAccepted): Checked {
        val existing = state.quests.find { it.questId == change.questId }
        if (existing != null && existing.period == change.period && existing.status != QuestStatus.ABANDONED) {
            return Checked.Failed("quest ${change.questId} is already ${existing.status} in period ${change.period}")
        }
        val accepted = QuestStateDto(change.questId, QuestStatus.ACTIVE, 0, change.stepVersion, change.period, null)
        return Checked.Ok(
            state.copy(
                quests = state.quests.filterNot { it.questId == change.questId } + accepted,
                counters = state.counters.filterNot { it.questId == change.questId }
            )
        )
    }

    /** Applies [transform] to the quest [questId] if it is active and on [fromStep]. */
    private inline fun withActiveQuest(
        state: ProgressionStateDto,
        questId: String,
        fromStep: Int,
        transform: (QuestStateDto) -> QuestStateDto
    ): Checked {
        val quest = state.quests.find { it.questId == questId }
            ?: return Checked.Failed("quest $questId was not accepted")
        if (quest.status != QuestStatus.ACTIVE) return Checked.Failed("quest $questId is ${quest.status}")
        if (quest.step != fromStep) return Checked.Failed("quest $questId is on step ${quest.step}, not $fromStep")
        return Checked.Ok(state.copy(quests = state.quests.map { if (it.questId == questId) transform(it) else it }))
    }
}
