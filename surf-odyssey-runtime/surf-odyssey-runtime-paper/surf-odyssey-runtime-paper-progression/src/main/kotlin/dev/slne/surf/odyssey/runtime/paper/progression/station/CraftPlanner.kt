package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.CustomItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.MaterialRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition

/**
 * Crafting arithmetic of recipes: how often a recipe can be crafted from the carried items and which items a number
 * of crafts consumes.
 *
 * Carried items are given as counts by [countKey]: the item ID for custom items and the namespaced material key, such
 * as `minecraft:wheat`, for plain materials.
 */
object CraftPlanner {
    /**
     * Returns the key [ref] is counted under in inventory counts.
     *
     * @return the item ID of a custom item, or the namespaced key of a plain material
     */
    fun countKey(ref: ItemRef): String = when (ref) {
        is CustomItemRef -> ref.item.id
        is MaterialRef -> ref.material.key.asString()
    }

    /**
     * Returns how often [recipe] can be crafted from [inventoryCounts]. Ingredients naming the same item are summed.
     *
     * @param recipe the recipe to craft
     * @param inventoryCounts the carried amount of each item by [countKey]; missing keys count as zero
     * @return the number of complete ingredient sets, `0` if any ingredient is missing
     */
    fun maxCrafts(recipe: RecipeDefinition, inventoryCounts: Map<String, Int>): Int {
        val needed = neededPerCraft(recipe)
        if (needed.isEmpty()) return 0
        return needed.minOf { (key, amount) -> (inventoryCounts[key] ?: 0) / amount }
    }

    /**
     * Returns whether [inventoryCounts] hold at least one complete ingredient set of [recipe].
     *
     * @param recipe the recipe to craft
     * @param inventoryCounts the carried amount of each item by [countKey]
     */
    fun canCraft(recipe: RecipeDefinition, inventoryCounts: Map<String, Int>): Boolean =
        maxCrafts(recipe, inventoryCounts) > 0

    /**
     * Returns the ingredients consumed by [times] crafts of [recipe], in recipe order.
     *
     * @param recipe the crafted recipe
     * @param times how many crafts are made
     */
    fun ingredientsFor(recipe: RecipeDefinition, times: Int): List<Ingredient> =
        recipe.ingredients.map { Ingredient(it.item, it.amount * times) }

    /** Returns the amount one craft of [recipe] consumes of each item, by [countKey]. */
    private fun neededPerCraft(recipe: RecipeDefinition): Map<String, Int> =
        recipe.ingredients.groupingBy { countKey(it.item) }.fold(0) { sum, ingredient -> sum + ingredient.amount }
}
