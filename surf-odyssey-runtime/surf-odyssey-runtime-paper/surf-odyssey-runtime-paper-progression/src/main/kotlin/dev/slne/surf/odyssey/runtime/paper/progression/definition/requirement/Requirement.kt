package dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement

import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType

/** Read-only view of one player's progression state. */
interface ProgressionView {
    /**
     * Returns the player's state of a quest.
     *
     * @param questId the quest ID
     * @return the state, or `null` if the player never accepted the quest
     */
    fun questState(questId: String): QuestStateView?

    /**
     * Returns whether the player has unlocked a recipe.
     *
     * @param recipeId the recipe ID
     */
    fun isRecipeUnlocked(recipeId: String): Boolean

    /**
     * Returns whether the player owns a title.
     *
     * @param titleId the title ID
     */
    fun hasTitle(titleId: String): Boolean
}

/**
 * One player's state of one quest.
 *
 * @property status lifecycle state of the quest
 * @property step index of the current step
 * @property period index of the period the state belongs to; only meaningful for recurring quests
 */
data class QuestStateView(val status: QuestStatus, val step: Int, val period: Long)

/** Everything a [Requirement] may inspect about one player. */
interface RequirementContext {
    /** The player's progression state. */
    val progression: ProgressionView

    /**
     * Counts the items in the player's inventory that match [ref].
     *
     * @param ref the item to count
     * @return the total amount over all matching stacks
     */
    fun countItems(ref: ItemRef): Int

    /**
     * Returns the level of the progression tool the player holds in the main hand.
     *
     * @param type the tool type to look for
     * @return the level, or `null` if the player holds no tool of this type
     */
    fun toolLevelInHand(type: ToolType): Int?
}

/** A condition on one player, used to gate quests, answers, NPC visibility and regions. */
fun interface Requirement {
    /**
     * Checks the condition.
     *
     * @param context the player to check
     * @return `true` if the condition holds
     */
    fun isMet(context: RequirementContext): Boolean
}

/** Factories for the built-in [Requirement]s. */
object Requirements {
    /** Requirement that always holds. */
    val ALWAYS: Requirement = Requirement { true }

    /**
     * Holds when the player has completed [quest].
     *
     * @param quest the quest to check
     */
    fun questCompleted(quest: QuestDefinition): Requirement = Requirement { context ->
        context.progression.questState(quest.id)?.status == QuestStatus.COMPLETED
    }

    /**
     * Holds when [quest] is active at step [step] or later, or completed.
     *
     * @param quest the quest to check
     * @param step the minimum step index
     */
    fun questStepReached(quest: QuestDefinition, step: Int): Requirement = Requirement { context ->
        val state = context.progression.questState(quest.id)
        when (state?.status) {
            QuestStatus.COMPLETED -> true
            QuestStatus.ACTIVE -> state.step >= step
            QuestStatus.ABANDONED, null -> false
        }
    }

    /**
     * Holds when the player has unlocked [recipe].
     *
     * @param recipe the recipe to check
     */
    fun recipeUnlocked(recipe: RecipeDefinition): Requirement = Requirement { context ->
        context.progression.isRecipeUnlocked(recipe.id)
    }

    /**
     * Holds when the player carries at least [amount] of [item].
     *
     * @param item the item to count
     * @param amount the minimum amount
     */
    fun hasItems(item: ItemRef, amount: Int): Requirement = Requirement { context ->
        context.countItems(item) >= amount
    }

    /**
     * Holds when the player holds a tool of [type] with at least [level] in the main hand.
     *
     * @param type the tool type
     * @param level the minimum tool level
     */
    fun toolLevelInHand(type: ToolType, level: Int): Requirement = Requirement { context ->
        val held = context.toolLevelInHand(type)
        held != null && held >= level
    }

    /**
     * Holds when every requirement in [requirements] holds; holds for an empty list.
     *
     * @param requirements the combined requirements
     */
    fun allOf(vararg requirements: Requirement): Requirement {
        val all = requirements.toList()
        return Requirement { context -> all.all { it.isMet(context) } }
    }

    /**
     * Holds when at least one requirement in [requirements] holds; never holds for an empty list.
     *
     * @param requirements the combined requirements
     */
    fun anyOf(vararg requirements: Requirement): Requirement {
        val any = requirements.toList()
        return Requirement { context -> any.any { it.isMet(context) } }
    }

    /**
     * Holds when [requirement] does not hold.
     *
     * @param requirement the negated requirement
     */
    fun not(requirement: Requirement): Requirement = Requirement { context -> !requirement.isMet(context) }
}
