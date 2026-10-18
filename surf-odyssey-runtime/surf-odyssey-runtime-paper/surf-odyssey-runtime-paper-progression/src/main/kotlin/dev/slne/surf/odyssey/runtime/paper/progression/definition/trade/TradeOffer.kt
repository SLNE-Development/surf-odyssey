package dev.slne.surf.odyssey.runtime.paper.progression.definition.trade

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition

/** What a player receives from a trade. */
sealed interface TradeResult {
    /**
     * Gives items.
     *
     * @property item the item given
     * @property amount how many are given
     */
    data class Item(val item: ItemRef, val amount: Int) : TradeResult

    /**
     * Unlocks a recipe.
     *
     * @property recipe the unlocked recipe
     */
    data class Recipe(val recipe: RecipeDefinition) : TradeResult
}

/** What a player pays for a trade. */
sealed interface Price {
    /**
     * Pays with items.
     *
     * @property ingredients the items taken from the player
     */
    data class Items(val ingredients: List<Ingredient>) : Price

    /**
     * Pays with a currency.
     *
     * @property currencyId ID of the currency
     * @property amount the paid amount
     */
    data class Currency(val currencyId: String, val amount: Double) : Price
}

/**
 * One offer of a trader NPC.
 *
 * @property id ID of the offer, unique among the offers of its NPC
 * @property result what the player receives
 * @property price what the player pays
 */
class TradeOffer(val id: String, val result: TradeResult, val price: Price) {
    /** Returns [id] for log and validation messages. */
    override fun toString(): String = "TradeOffer($id)"
}
