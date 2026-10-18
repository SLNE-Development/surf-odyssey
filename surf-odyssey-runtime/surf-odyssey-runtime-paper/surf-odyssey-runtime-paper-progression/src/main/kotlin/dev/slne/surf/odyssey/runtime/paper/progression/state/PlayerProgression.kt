package dev.slne.surf.odyssey.runtime.paper.progression.state

import dev.slne.surf.odyssey.core.common.progression.CounterDeltaDto
import dev.slne.surf.odyssey.core.common.progression.CounterDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.ProgressionView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestStateView
import java.time.Clock

/**
 * The cached progression of one player on this server.
 *
 * The cache starts from the state loaded from the progression service and follows the changes of committed
 * operations and the counter deltas counted here, so reads never wait for the service. All functions are safe to
 * call from several threads.
 *
 * @param initial the loaded state
 * @param clock the clock that stamps locally completed quests
 */
class PlayerProgression(initial: ProgressionStateDto, private val clock: Clock) : ProgressionView {
    /** The current cached state. */
    @Volatile
    var state: ProgressionStateDto = initial
        private set

    /** Counts the local changes made to the cached state; a reload uses it to detect changes made meanwhile. */
    @Volatile
    var revision: Long = 0
        private set

    /** Returns the cached state of the quest, or `null` if the player never accepted it. */
    override fun questState(questId: String): QuestStateView? =
        state.quests.find { it.questId == questId }?.let { QuestStateView(it.status, it.step, it.period) }

    /** Returns whether the recipe is unlocked in the cached state. */
    override fun isRecipeUnlocked(recipeId: String): Boolean = recipeId in state.recipes

    /** Returns whether the title is unlocked in the cached state. */
    override fun hasTitle(titleId: String): Boolean = titleId in state.titles

    /**
     * Returns the cached value of one objective counter.
     *
     * @return the value, or `0` if the counter does not exist
     */
    fun counter(questId: String, step: Int, stepVersion: Int, objective: Int, period: Long): Int =
        state.counters.find { CounterKey.of(it) == CounterKey(questId, step, stepVersion, objective, period) }?.value
            ?: 0

    /**
     * Applies the changes of a committed operation to the cached state, in order. A change that does not fit the
     * cached state, such as advancing a quest that is not on the expected step, is skipped.
     *
     * @param changes the committed changes
     */
    @Synchronized
    fun apply(changes: List<StateChange>) {
        state = changes.fold(state, ::applyChange)
        revision++
    }

    /**
     * Adds a counter delta to the cached counter, creating the counter if it does not exist.
     *
     * @param delta the delta to add
     */
    @Synchronized
    fun addCounter(delta: CounterDeltaDto) {
        state = state.copy(counters = addDelta(state.counters, delta))
        revision++
    }

    /**
     * Marks the active quest [questId] as abandoned and drops its counters. A quest that is not active is left as it is.
     *
     * @param questId the id of the quest definition
     */
    @Synchronized
    fun abandon(questId: String) {
        val quest = state.quests.find { it.questId == questId && it.status == QuestStatus.ACTIVE } ?: return
        state = state.copy(
            quests = state.quests.map { if (it === quest) it.copy(status = QuestStatus.ABANDONED) else it },
            counters = state.counters.filterNot { it.questId == questId }
        )
        revision++
    }

    /**
     * Sets the cached tracked quest.
     *
     * @param questId the id of the tracked quest, or `null` for none
     */
    @Synchronized
    fun setTrackedQuest(questId: String?) {
        state = state.copy(trackedQuest = questId)
        revision++
    }

    /**
     * Replaces the cached state with a freshly loaded one and adds the deltas that were not sent yet on top.
     *
     * @param loaded the state loaded from the progression service
     * @param pending the buffered counter deltas the loaded state does not contain yet
     * @param expectedRevision the [revision] read before loading, or `null` to replace unconditionally
     * @return `false` without replacing anything if [revision] no longer equals [expectedRevision]
     */
    @Synchronized
    fun replace(loaded: ProgressionStateDto, pending: List<CounterDeltaDto>, expectedRevision: Long? = null): Boolean {
        if (expectedRevision != null && expectedRevision != revision) return false
        state = loaded.copy(counters = pending.fold(loaded.counters, ::addDelta))
        return true
    }

    /** Returns [counters] with [delta] added to its counter. */
    private fun addDelta(counters: List<CounterDto>, delta: CounterDeltaDto): List<CounterDto> {
        val key = CounterKey.of(delta)
        val existing = counters.find { CounterKey.of(it) == key }
        return if (existing == null) {
            counters + CounterDto(key.questId, key.step, key.stepVersion, key.objective, key.period, delta.delta)
        } else {
            counters.map { if (it === existing) it.copy(value = it.value + delta.delta) else it }
        }
    }

    /** Returns [current] with [change] applied, or [current] unchanged if the change does not fit. */
    private fun applyChange(current: ProgressionStateDto, change: StateChange): ProgressionStateDto = when (change) {
        is StateChange.QuestAccepted -> current.copy(
            quests = current.quests.filterNot { it.questId == change.questId } +
                    QuestStateDto(change.questId, QuestStatus.ACTIVE, 0, change.stepVersion, change.period, null),
            counters = current.counters.filterNot { it.questId == change.questId }
        )
        is StateChange.QuestStepAdvanced -> withActiveQuest(current, change.questId, change.fromStep) {
            it.copy(step = change.toStep, stepVersion = change.stepVersion)
        }
        is StateChange.QuestCompleted -> withActiveQuest(current, change.questId, change.fromStep) {
            it.copy(status = QuestStatus.COMPLETED, completedAt = clock.millis())
        }
        is StateChange.CounterSet -> {
            val key = CounterKey(change.questId, change.step, change.stepVersion, change.objective, change.period)
            current.copy(
                counters = current.counters.filterNot { CounterKey.of(it) == key } +
                        CounterDto(key.questId, key.step, key.stepVersion, key.objective, key.period, change.value)
            )
        }
        is StateChange.RecipeUnlocked -> current.copy(recipes = current.recipes + change.recipeId)
        is StateChange.TitleUnlocked -> current.copy(titles = current.titles + change.titleId)
        is StateChange.JobStarted -> current.copy(jobs = current.jobs + change.job)
        is StateChange.JobCollected -> current.copy(jobs = current.jobs.filterNot { it.id == change.jobId })
    }

    /** Applies [transform] to the quest [questId] if it is active on [fromStep], otherwise returns [current]. */
    private inline fun withActiveQuest(
        current: ProgressionStateDto,
        questId: String,
        fromStep: Int,
        transform: (QuestStateDto) -> QuestStateDto
    ): ProgressionStateDto = current.copy(quests = current.quests.map {
        if (it.questId == questId && it.status == QuestStatus.ACTIVE && it.step == fromStep) transform(it) else it
    })
}

