package dev.slne.surf.odyssey.runtime.paper.progression.definition.quest

import dev.slne.surf.odyssey.runtime.paper.progression.definition.Definition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.ProgressionDsl
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.DialogNode
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.DialogNodeBuilder
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirements
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.QuestReward
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.EntityType

/**
 * One step of a quest.
 *
 * @property version version of the step; when it changes, open counters of this step start again from zero
 * @property description what the player has to do, shown in the quest book
 * @property npc the NPC the step is turned in at
 * @property entryNode node shown when the player talks to [npc] during this step
 * @property objectives goals that all have to be met
 * @property rewards rewards given when this step is turned in
 */
class QuestStep(
    val version: Int,
    val description: Component,
    val npc: NpcDefinition,
    val entryNode: String,
    val objectives: List<Objective>,
    val rewards: List<QuestReward> = emptyList(),
)

/**
 * A quest with its steps, rewards and conversation nodes.
 *
 * @property id stable quest ID
 * @property type kind of quest
 * @property title quest title
 * @property description quest description
 * @property giver the NPC offering the quest
 * @property offerNode node shown when the giver offers the quest
 * @property requirement requirement a player must meet to be offered the quest
 * @property steps steps in order
 * @property rewards rewards given when the quest is completed
 * @property nodes conversation nodes of the quest
 */
class QuestDefinition(
    override val id: String,
    val type: QuestType,
    val title: Component,
    val description: Component,
    val giver: NpcDefinition,
    val offerNode: String,
    val requirement: Requirement,
    val steps: List<QuestStep>,
    val rewards: List<QuestReward>,
    val nodes: List<DialogNode>,
) : Definition {
    /** Returns [id] for log and validation messages. */
    override fun toString(): String = "QuestDefinition($id)"
}

/**
 * Builds the values of a [QuestStep].
 *
 * @property version the step version
 * @property npc the NPC the step is turned in at
 * @property entryNode the node shown during this step
 */
@ProgressionDsl
class QuestStepBuilder(private val version: Int, private val npc: NpcDefinition, private val entryNode: String) {
    /** What the player has to do, shown in the quest book. */
    var description: Component = Component.empty()

    /** Objectives added so far. */
    private val objectives = mutableListOf<Objective>()

    /** Step rewards added so far. */
    private val rewards = mutableListOf<QuestReward>()

    /**
     * Adds an objective.
     *
     * @param objective the objective
     */
    fun objective(objective: Objective) {
        objectives += objective
    }

    /** Adds a [Objective.Deliver] objective. */
    fun deliver(item: ItemRef, amount: Int, npc: NpcDefinition) = objective(Objective.Deliver(item, amount, npc))

    /** Adds a [Objective.TalkTo] objective. */
    fun talkTo(npc: NpcDefinition, nodeId: String) = objective(Objective.TalkTo(npc, nodeId))

    /** Adds a [Objective.Harvest] objective. */
    fun harvest(material: Material, amount: Int, regionId: String? = null) =
        objective(Objective.Harvest(material, amount, regionId))

    /** Adds a [Objective.Fish] objective. */
    fun fish(amount: Int, fishId: String? = null, material: Material? = null, regionId: String? = null) =
        objective(Objective.Fish(fishId, material, amount, regionId))

    /** Adds a [Objective.Kill] objective. */
    fun kill(entity: EntityType, amount: Int, regionId: String? = null) =
        objective(Objective.Kill(entity, amount, regionId))

    /** Adds a [Objective.Craft] objective. */
    fun craft(recipe: RecipeDefinition, amount: Int) = objective(Objective.Craft(recipe, amount))

    /**
     * Adds a reward given when this step is turned in.
     *
     * @param reward the reward
     */
    fun reward(reward: QuestReward) {
        rewards += reward
    }

    /** Creates the step. */
    fun build(): QuestStep = QuestStep(version, description, npc, entryNode, objectives.toList(), rewards.toList())
}

/**
 * Builds the values of a [QuestDefinition].
 *
 * @property id the quest ID
 * @property type the quest type
 * @property giver the NPC offering the quest
 */
@ProgressionDsl
class QuestBuilder(private val id: String, private val type: QuestType, private val giver: NpcDefinition) {
    /** Quest title; required. */
    var title: Component? = null

    /** Quest description. */
    var description: Component = Component.empty()

    /** Node shown when the giver offers the quest; required. */
    var offerNode: String? = null

    /** Requirement to be offered the quest. */
    var requirement: Requirement = Requirements.ALWAYS

    /** Steps added so far. */
    private val steps = mutableListOf<QuestStep>()

    /** Quest rewards added so far. */
    private val rewards = mutableListOf<QuestReward>()

    /** Nodes added so far. */
    private val nodes = mutableListOf<DialogNode>()

    /**
     * Adds the next step.
     *
     * @param version the step version
     * @param npc the NPC the step is turned in at
     * @param entryNode the node shown during this step
     * @param block sets description, objectives and step rewards
     */
    fun step(version: Int, npc: NpcDefinition, entryNode: String, block: QuestStepBuilder.() -> Unit) {
        steps += QuestStepBuilder(version, npc, entryNode).apply(block).build()
    }

    /**
     * Adds a conversation node.
     *
     * @param id the node ID, unique within this quest
     * @param npc the speaking NPC
     * @param block sets mood, text and answers
     */
    fun node(id: String, npc: NpcDefinition, block: DialogNodeBuilder.() -> Unit) {
        nodes += DialogNodeBuilder(id, npc).apply(block).build()
    }

    /**
     * Adds a reward given when the quest is completed.
     *
     * @param reward the reward
     */
    fun reward(reward: QuestReward) {
        rewards += reward
    }

    /**
     * Creates the definition.
     *
     * @throws IllegalStateException if [title] or [offerNode] is missing
     */
    fun build(): QuestDefinition = QuestDefinition(
        id = id,
        type = type,
        title = checkNotNull(title) { "quest $id needs a title" },
        description = description,
        giver = giver,
        offerNode = checkNotNull(offerNode) { "quest $id needs an offerNode" },
        requirement = requirement,
        steps = steps.toList(),
        rewards = rewards.toList(),
        nodes = nodes.toList(),
    )
}

/**
 * Defines a quest.
 *
 * @param id stable quest ID
 * @param type kind of quest
 * @param giver the NPC offering the quest
 * @param block sets texts, requirement, steps, rewards and nodes
 * @return the quest definition
 */
fun quest(id: String, type: QuestType, giver: NpcDefinition, block: QuestBuilder.() -> Unit): QuestDefinition =
    QuestBuilder(id, type, giver).apply(block).build()
