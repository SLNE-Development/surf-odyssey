package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.Objective
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import org.bukkit.Material
import org.bukkit.entity.EntityType

/**
 * One objective of the current step of an active quest.
 *
 * @property quest the quest
 * @property step the index of the step
 * @property objective the index of the objective within the step
 * @property period the period index the objective's counter belongs to
 * @property definition the objective
 */
data class ActiveObjective(
    val quest: QuestDefinition,
    val step: Int,
    val objective: Int,
    val period: Long,
    val definition: Objective,
) {
    /** The version of the step, which the objective's counter is stored under. */
    val stepVersion: Int
        get() = quest.steps[step].version
}

/**
 * An objective an event advances.
 *
 * @property objective the advanced objective
 * @property amount how much the event adds to the objective's counter
 */
data class ObjectiveMatch(val objective: ActiveObjective, val amount: Int)

/**
 * The event-driven objectives of a player's active quests, indexed by the kind of event that advances them.
 *
 * `Deliver` and `TalkTo` objectives are not advanced by events and are not indexed.
 */
class ObjectiveIndex private constructor(
    private val harvest: Map<Material, List<ActiveObjective>>,
    private val kill: Map<EntityType, List<ActiveObjective>>,
    private val craft: Map<String, List<ActiveObjective>>,
    private val fish: List<ActiveObjective>,
) {
    /** Returns whether no objective is indexed. */
    fun isEmpty(): Boolean = harvest.isEmpty() && kill.isEmpty() && craft.isEmpty() && fish.isEmpty()

    /**
     * Returns the objectives [event] advances, in the order of the quests and objectives given to [build].
     *
     * An objective with a region only matches events in that region. Crafts add the crafted amount, every other
     * event adds one.
     *
     * @param event what the player did
     */
    fun matching(event: ProgressEvent): List<ObjectiveMatch> = when (event) {
        is ProgressEvent.Harvested -> harvest[event.material].orEmpty()
            .filter { inRegion((it.definition as Objective.Harvest).regionId, event.regionId) }
            .map { ObjectiveMatch(it, 1) }
        is ProgressEvent.Killed -> kill[event.entity].orEmpty()
            .filter { inRegion((it.definition as Objective.Kill).regionId, event.regionId) }
            .map { ObjectiveMatch(it, 1) }
        is ProgressEvent.Crafted -> craft[event.recipeId].orEmpty().map { ObjectiveMatch(it, event.amount) }
        is ProgressEvent.Fished -> fish
            .filter {
                val objective = it.definition as Objective.Fish
                (objective.fishId == null || objective.fishId == event.fishId) &&
                        (objective.material == null || objective.material == event.material) &&
                        inRegion(objective.regionId, event.regionId)
            }
            .map { ObjectiveMatch(it, 1) }
    }

    /** Returns whether an event in [actual] counts for an objective limited to [required]. */
    private fun inRegion(required: String?, actual: String?): Boolean = required == null || required == actual

    /** Builds indices. */
    companion object {
        /**
         * Indexes the event-driven objectives of the current steps of [quests].
         *
         * @param quests the active quests
         */
        fun build(quests: List<ActiveQuest>): ObjectiveIndex {
            val harvest = LinkedHashMap<Material, MutableList<ActiveObjective>>()
            val kill = LinkedHashMap<EntityType, MutableList<ActiveObjective>>()
            val craft = LinkedHashMap<String, MutableList<ActiveObjective>>()
            val fish = mutableListOf<ActiveObjective>()
            for (active in quests) {
                val step = active.quest.steps.getOrNull(active.step) ?: continue
                step.objectives.forEachIndexed { index, objective ->
                    val entry = ActiveObjective(active.quest, active.step, index, active.period, objective)
                    when (objective) {
                        is Objective.Harvest -> harvest.getOrPut(objective.material) { mutableListOf() } += entry
                        is Objective.Kill -> kill.getOrPut(objective.entity) { mutableListOf() } += entry
                        is Objective.Craft -> craft.getOrPut(objective.recipe.id) { mutableListOf() } += entry
                        is Objective.Fish -> fish += entry
                        is Objective.Deliver, is Objective.TalkTo -> Unit
                    }
                }
            }
            return ObjectiveIndex(harvest, kill, craft, fish)
        }
    }
}
