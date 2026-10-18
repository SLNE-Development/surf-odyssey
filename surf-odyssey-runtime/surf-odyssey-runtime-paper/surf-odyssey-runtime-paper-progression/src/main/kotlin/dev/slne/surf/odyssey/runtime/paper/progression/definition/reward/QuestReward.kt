package dev.slne.surf.odyssey.runtime.paper.progression.definition.reward

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.title.TitleDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition

/** Something a player receives for finishing a quest or a quest step. */
interface QuestReward {
    /** Key naming the kind of reward, such as `item` or `currency`. */
    val typeKey: String
}

/**
 * Gives items.
 *
 * @property item the item given
 * @property amount how many are given
 */
data class ItemReward(val item: ItemRef, val amount: Int) : QuestReward {
    /** Always `item`. */
    override val typeKey: String = "item"
}

/**
 * Unlocks a recipe.
 *
 * @property recipe the unlocked recipe
 */
data class RecipeReward(val recipe: RecipeDefinition) : QuestReward {
    /** Always `recipe`. */
    override val typeKey: String = "recipe"
}

/**
 * Grants a title.
 *
 * @property title the granted title
 */
data class TitleReward(val title: TitleDefinition) : QuestReward {
    /** Always `title`. */
    override val typeKey: String = "title"
}

/**
 * Gives a progression tool.
 *
 * @property tool the given tool
 */
data class ToolReward(val tool: ToolDefinition) : QuestReward {
    /** Always `tool`. */
    override val typeKey: String = "tool"
}

/**
 * Pays an amount of a currency.
 *
 * @property currencyId ID of the currency
 * @property amount the paid amount
 */
data class CurrencyReward(val currencyId: String, val amount: Double) : QuestReward {
    /** Always `currency`. */
    override val typeKey: String = "currency"
}
