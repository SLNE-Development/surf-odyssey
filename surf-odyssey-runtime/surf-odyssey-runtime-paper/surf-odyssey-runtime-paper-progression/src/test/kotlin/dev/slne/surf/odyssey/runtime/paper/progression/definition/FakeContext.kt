package dev.slne.surf.odyssey.runtime.paper.progression.definition

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.ProgressionView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestStateView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.RequirementContext
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType

/**
 * In-memory [RequirementContext] for tests.
 *
 * @property quests quest states by quest ID; quests that are absent were never accepted
 * @property recipes IDs of unlocked recipes
 * @property titles IDs of owned titles
 * @property items item counts in the inventory by reference
 * @property tools level of the tool held in hand by tool type
 */
class FakeContext(
    val quests: Map<String, QuestStateView> = emptyMap(),
    val recipes: Set<String> = emptySet(),
    val titles: Set<String> = emptySet(),
    val items: Map<ItemRef, Int> = emptyMap(),
    val tools: Map<ToolType, Int> = emptyMap(),
) : RequirementContext {
    /** View over [quests], [recipes] and [titles]. */
    override val progression: ProgressionView = object : ProgressionView {
        /** Returns the state stored in [quests]. */
        override fun questState(questId: String): QuestStateView? = quests[questId]

        /** Returns whether [recipeId] is in [recipes]. */
        override fun isRecipeUnlocked(recipeId: String): Boolean = recipeId in recipes

        /** Returns whether [titleId] is in [titles]. */
        override fun hasTitle(titleId: String): Boolean = titleId in titles
    }

    /** Returns the count stored in [items], or zero. */
    override fun countItems(ref: ItemRef): Int = items[ref] ?: 0

    /** Returns the level stored in [tools]. */
    override fun toolLevelInHand(type: ToolType): Int? = tools[type]
}
