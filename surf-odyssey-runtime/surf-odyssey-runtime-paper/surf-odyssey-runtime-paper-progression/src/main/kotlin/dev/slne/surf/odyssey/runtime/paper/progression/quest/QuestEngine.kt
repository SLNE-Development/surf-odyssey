package dev.slne.surf.odyssey.runtime.paper.progression.quest

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.slne.surf.odyssey.core.common.progression.CounterDeltaDto
import dev.slne.surf.odyssey.core.common.progression.QuestStateDto
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMode
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionRuntime
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.Objective
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationOutcome
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationPlan
import dev.slne.surf.odyssey.runtime.paper.progression.state.PlayerProgression
import dev.slne.surf.odyssey.runtime.paper.progression.state.PlayerRequirementContext
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Runs the quests of the players on this server: accepting, counting objective progress, delivering, turning in,
 * abandoning and tracking.
 *
 * Accepting, talking, delivering and turning in are item operations and only run in [ProgressionMode.FULL];
 * abandoning and tracking also run in [ProgressionMode.VIEW_ONLY]. Only one quest action per player runs at a time.
 * When the tracked quest is completed or abandoned, the next active quest is tracked, main quests first; an accepted
 * quest is tracked when no active quest is tracked.
 */
object QuestEngine {
    /** The players with a running quest action. */
    private val running: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /** The objective index of each cached progression, dropped together with the progression. */
    private val indices: MutableMap<PlayerProgression, CachedIndex> =
        Collections.synchronizedMap(WeakHashMap())

    /**
     * An objective index and what it was built from.
     *
     * @property quests the quest states the index was built from
     * @property day the daily period the index was built in; weekly and monthly periods change only with it
     * @property index the index
     */
    private class CachedIndex(val quests: List<QuestStateDto>, val day: Long, val index: ObjectiveIndex)

    /**
     * Counts [event] for the matching objectives of the active quests of [player]. Does nothing outside of
     * [ProgressionMode.FULL] or while the player's state is not loaded.
     *
     * Counters stop at their objective's amount. When an objective is met, the player gets a message and a sound.
     * The counted deltas are buffered and sent with the next flush.
     *
     * @param player the player who did something
     * @param event what the player did
     */
    fun onProgress(player: Player, event: ProgressEvent) {
        val runtime = Progression.runtimeOrNull ?: return
        if (runtime.mode != ProgressionMode.FULL) return
        val progression = runtime.store[player.uniqueId] ?: return
        val matches = indexOf(progression, runtime, runtime.clock.instant()).matching(event)
        if (matches.isEmpty()) return
        val announced = mutableSetOf<Pair<String, Int>>()
        for (match in matches) {
            val objective = match.objective
            val amount = objective.definition.amount
            val current = progression.counter(
                objective.quest.id, objective.step, objective.stepVersion, objective.objective, objective.period
            )
            if (current >= amount || match.amount <= 0) continue
            val added = minOf(match.amount, amount - current)
            runtime.store.addCounter(
                player.uniqueId,
                CounterDeltaDto(
                    objective.quest.id, objective.step, objective.stepVersion, objective.objective, objective.period,
                    added
                )
            )
            if (current + added >= amount && announced.add(objective.quest.id to objective.step)) {
                ProgressionMessages.objectiveDone(player, objective.quest.steps[objective.step].description)
            }
        }
    }

    /**
     * Accepts [quest] for [player] if its giver offers it to the player, by the rules of [QuestAvailability]. When
     * the player tracks no active quest, the accepted quest becomes the tracked one.
     *
     * @return the outcome of the operation; [OperationOutcome.Rejected] without an operation if the quest is not
     *         offered or the server does not run [ProgressionMode.FULL]
     */
    suspend fun accept(player: Player, quest: QuestDefinition): OperationOutcome = guarded(player) { runtime, progression ->
        val now = runtime.clock.instant()
        val context = PlayerRequirementContext(player, progression)
        if (!QuestAvailability.isOffered(quest, progression, context, player.uniqueId, now, runtime.registry)) {
            return@guarded OperationOutcome.Rejected("quest ${quest.id} is not offered")
        }
        val outcome = runtime.operations.run(player, QuestPlans.accept(quest, QuestProgressView.periodOf(quest, now)))
        if (outcome == OperationOutcome.Success &&
            !QuestProgressView.tracksActiveQuest(runtime.registry, progression.state, runtime.clock.instant())
        ) {
            runtime.store.setTrackedQuest(player.uniqueId, quest.id)
        }
        outcome
    }

    /**
     * Fulfils the open `TalkTo` objectives of the current step of [quest].
     *
     * @return the outcome of the operation; [OperationOutcome.Rejected] without an operation if the quest is not
     *         active or has no open `TalkTo` objective
     */
    suspend fun completeTalk(player: Player, quest: QuestDefinition): OperationOutcome =
        guarded(player) { runtime, progression ->
            val (state, progress) = currentOf(runtime, progression, quest)
                ?: return@guarded OperationOutcome.Rejected("quest ${quest.id} is not active")
            val plan = QuestPlans.completeTalk(quest, state, progress)
                ?: return@guarded OperationOutcome.Rejected("quest ${quest.id} has no open talk objective")
            runtime.operations.run(player, plan).also {
                if (it == OperationOutcome.Success) announceStep(player, quest, state.step)
            }
        }

    /**
     * Hands in the items of the open `Deliver` objectives of the current step of [quest] that target [npc]. Only
     * the items still missing are taken, and the objectives are then met.
     *
     * @return the outcome of the operation; [OperationOutcome.MissingItems] if the player lacks some of the items,
     *         [OperationOutcome.Rejected] without an operation if the quest is not active or nothing is open at [npc]
     */
    suspend fun deliver(player: Player, quest: QuestDefinition, npc: NpcDefinition): OperationOutcome =
        guarded(player) { runtime, progression ->
            val (state, progress) = currentOf(runtime, progression, quest)
                ?: return@guarded OperationOutcome.Rejected("quest ${quest.id} is not active")
            val plan = QuestPlans.deliver(quest, state, progress, npc)
                ?: return@guarded OperationOutcome.Rejected("quest ${quest.id} has nothing to deliver to ${npc.id}")
            runtime.operations.run(player, plan).also {
                if (it == OperationOutcome.Success) announceStep(player, quest, state.step)
            }
        }

    /**
     * Turns in the current step of [quest] when all of its objectives are met: advances to the next step with the
     * step's rewards, or completes the quest after its last step with the step's and the quest's rewards.
     *
     * @return the outcome of the operation; [OperationOutcome.Rejected] without an operation if the quest is not
     *         active or an objective is still open
     */
    suspend fun turnIn(player: Player, quest: QuestDefinition): OperationOutcome = guarded(player) { runtime, progression ->
        val (state, progress) = currentOf(runtime, progression, quest)
            ?: return@guarded OperationOutcome.Rejected("quest ${quest.id} is not active")
        val plan = QuestPlans.turnIn(quest, state, progress)
            ?: return@guarded OperationOutcome.Rejected("quest ${quest.id} has open objectives")
        val outcome = runtime.operations.run(player, plan)
        if (outcome == OperationOutcome.Success && state.step == quest.steps.lastIndex) {
            ProgressionMessages.questCompleted(player, quest.title)
            if (progression.state.trackedQuest == quest.id) trackNext(player, runtime, progression)
        }
        outcome
    }

    /**
     * Abandons [quest] for [player] and discards its progress; items already handed in are not returned. Only
     * quests whose type is abandonable and that are active in the current period can be abandoned.
     *
     * @return `true` if the quest is now abandoned
     */
    suspend fun abandon(player: Player, quest: QuestDefinition): Boolean {
        if (!quest.type.abandonable) return false
        val runtime = Progression.runtimeOrNull ?: return false
        val progression = runtime.store[player.uniqueId]
        if (progression == null || !runtime.store.isAvailableFor(player.uniqueId)) {
            ProgressionMessages.unavailable(player)
            return false
        }
        val state = progression.state.quests.find { it.questId == quest.id }
        if (!QuestProgressView.isCurrent(quest, state, QuestProgressView.periodOf(quest, runtime.clock.instant()))) {
            return false
        }
        if (!running.add(player.uniqueId)) return false
        try {
            val abandoned = runtime.store.abandon(player.uniqueId, quest.id)
            if (!abandoned && !Progression.isAvailable) ProgressionMessages.unavailable(player)
            if (abandoned && progression.state.trackedQuest == quest.id) trackNext(player, runtime, progression)
            return abandoned
        } finally {
            running.remove(player.uniqueId)
        }
    }

    /**
     * Tracks [quest] for [player], or tracks nothing when [quest] is `null`. A quest that is not active in the
     * current period is not tracked.
     */
    suspend fun track(player: Player, quest: QuestDefinition?) {
        val runtime = Progression.runtimeOrNull ?: return
        val progression = runtime.store[player.uniqueId]
        if (progression == null || !runtime.store.isAvailableFor(player.uniqueId)) {
            ProgressionMessages.unavailable(player)
            return
        }
        if (quest != null) {
            val state = progression.state.quests.find { it.questId == quest.id }
            val period = QuestProgressView.periodOf(quest, runtime.clock.instant())
            if (!QuestProgressView.isCurrent(quest, state, period)) return
        }
        if (!runtime.store.setTrackedQuest(player.uniqueId, quest?.id)) ProgressionMessages.unavailable(player)
    }

    /**
     * Returns the progress of [quest] for [player].
     *
     * @return the progress, or `null` if the quest is not active in the current period or the player's state is not
     *         loaded
     */
    fun progressOf(player: Player, quest: QuestDefinition): QuestProgress? {
        val runtime = Progression.runtimeOrNull ?: return null
        val progression = runtime.store[player.uniqueId] ?: return null
        return currentOf(runtime, progression, quest)?.second
    }

    /**
     * Runs [action] for [player] on the player's thread while no other quest action of the player runs.
     *
     * @return [OperationOutcome.Busy] if another quest action runs, [OperationOutcome.Unavailable] if no engine is
     *         installed or the player's state is not loaded, [OperationOutcome.Rejected] outside of
     *         [ProgressionMode.FULL], otherwise the result of [action]
     */
    private suspend fun guarded(
        player: Player,
        action: suspend (ProgressionRuntime, PlayerProgression) -> OperationOutcome,
    ): OperationOutcome {
        val runtime = Progression.runtimeOrNull ?: return OperationOutcome.Unavailable
        if (runtime.mode != ProgressionMode.FULL) return OperationOutcome.Rejected("quests change only on hub servers")
        val progression = runtime.store[player.uniqueId] ?: return OperationOutcome.Unavailable
        if (!running.add(player.uniqueId)) return OperationOutcome.Busy
        try {
            return withContext(ProgressionModule.plugin.entityDispatcher(player)) { action(runtime, progression) }
        } finally {
            running.remove(player.uniqueId)
        }
    }

    /**
     * Returns the stored state and the progress of [quest] in [progression].
     *
     * @return both, or `null` if the quest is not active in the current period
     */
    private fun currentOf(
        runtime: ProgressionRuntime,
        progression: PlayerProgression,
        quest: QuestDefinition,
    ): Pair<QuestStateDto, QuestProgress>? {
        val state = progression.state
        val stored = state.quests.find { it.questId == quest.id } ?: return null
        val period = QuestProgressView.periodOf(quest, runtime.clock.instant())
        val progress = QuestProgressView.compute(quest, stored, state.counters, period) ?: return null
        return stored to progress
    }

    /** Tells [player] that an objective of [step] of [quest] is met. */
    private fun announceStep(player: Player, quest: QuestDefinition, step: Int) {
        ProgressionMessages.objectiveDone(player, quest.steps[step].description)
    }

    /** Tracks the next active quest of [player], or nothing if no quest is active. */
    private suspend fun trackNext(player: Player, runtime: ProgressionRuntime, progression: PlayerProgression) {
        val next = QuestProgressView.nextTracked(runtime.registry, progression.state, runtime.clock.instant())
        runtime.store.setTrackedQuest(player.uniqueId, next?.id)
    }

    /** Returns the objective index of [progression] at [now], building it when the active quests or the day changed. */
    private fun indexOf(progression: PlayerProgression, runtime: ProgressionRuntime, now: Instant): ObjectiveIndex {
        val state = progression.state
        val day = Periods.index(QuestType.DAILY, now)
        indices[progression]?.let { if (it.quests === state.quests && it.day == day) return it.index }
        val index = ObjectiveIndex.build(QuestProgressView.activeQuests(runtime.registry, state, now))
        indices[progression] = CachedIndex(state.quests, day, index)
        return index
    }
}

/** Builds the operation plans of quest transitions from a quest's state and progress. */
internal object QuestPlans {
    /** The source stored with the deliveries of quest operations. */
    const val SOURCE = "quest"

    /**
     * Plans accepting [quest]: the quest starts at its first step in [period].
     *
     * @param quest the quest
     * @param period the period the quest is accepted in, see [QuestProgressView.periodOf]
     */
    fun accept(quest: QuestDefinition, period: Long): OperationPlan = OperationPlan(
        take = emptyList(),
        changes = listOf(StateChange.QuestAccepted(quest.id, quest.steps.first().version, period)),
        rewards = emptyList(),
        source = SOURCE,
    )

    /**
     * Plans meeting the open `TalkTo` objectives of the current step.
     *
     * @param quest the quest
     * @param state the stored state of the active quest
     * @param progress the progress computed from [state]
     * @return the plan, or `null` if no `TalkTo` objective is open
     */
    fun completeTalk(quest: QuestDefinition, state: QuestStateDto, progress: QuestProgress): OperationPlan? {
        val step = quest.steps[progress.step]
        val open = step.objectives.withIndex()
            .filter { (index, objective) -> objective is Objective.TalkTo && progress.counts[index] < objective.amount }
        if (open.isEmpty()) return null
        val changes = open.map { (index, objective) -> counterSet(quest, state, progress.step, index, objective.amount) }
        return OperationPlan(emptyList(), changes, emptyList(), SOURCE)
    }

    /**
     * Plans handing in the open `Deliver` objectives of the current step that target [npc]: takes the missing amount
     * of each and sets its counter to the full amount.
     *
     * @param quest the quest
     * @param state the stored state of the active quest
     * @param progress the progress computed from [state]
     * @param npc the NPC the items are handed to
     * @return the plan, or `null` if no `Deliver` objective at [npc] is open
     */
    fun deliver(quest: QuestDefinition, state: QuestStateDto, progress: QuestProgress, npc: NpcDefinition): OperationPlan? {
        val step = quest.steps[progress.step]
        val open = step.objectives.withIndex().mapNotNull { (index, objective) ->
            if (objective !is Objective.Deliver || objective.npc.id != npc.id) return@mapNotNull null
            val missing = objective.amount - progress.counts[index]
            if (missing <= 0) null else Triple(index, objective, missing)
        }
        if (open.isEmpty()) return null
        return OperationPlan(
            take = open.map { (_, objective, missing) -> Ingredient(objective.item, missing) },
            changes = open.map { (index, objective, _) -> counterSet(quest, state, progress.step, index, objective.amount) },
            rewards = emptyList(),
            source = SOURCE,
        )
    }

    /**
     * Plans turning in the current step: advancing to the next step with the step's rewards, or completing the quest
     * after its last step with the step's and then the quest's rewards.
     *
     * @param quest the quest
     * @param state the stored state of the active quest
     * @param progress the progress computed from [state]
     * @return the plan, or `null` if an objective of the step is still open
     */
    fun turnIn(quest: QuestDefinition, state: QuestStateDto, progress: QuestProgress): OperationPlan? {
        if (!progress.complete) return null
        val step = quest.steps[progress.step]
        val next = progress.step + 1
        return if (next < quest.steps.size) {
            OperationPlan(
                emptyList(),
                listOf(StateChange.QuestStepAdvanced(state.questId, progress.step, next, quest.steps[next].version)),
                step.rewards,
                SOURCE,
            )
        } else {
            OperationPlan(
                emptyList(),
                listOf(StateChange.QuestCompleted(state.questId, progress.step)),
                step.rewards + quest.rewards,
                SOURCE,
            )
        }
    }

    /** Returns the change that sets counter [objective] of [step] of [quest] to [value]. */
    private fun counterSet(quest: QuestDefinition, state: QuestStateDto, step: Int, objective: Int, value: Int) =
        StateChange.CounterSet(quest.id, step, quest.steps[step].version, objective, state.period, value)
}
