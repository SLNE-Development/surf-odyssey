package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.recipe
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for crafting arithmetic. */
class CraftPlannerTest {
    @Test
    fun `max crafts is limited by the scarcest ingredient`() =
        assertEquals(3, CraftPlanner.maxCrafts(TestPacks.sushiRecipe(rice = 2, fish = 1), mapOf("rice" to 7, "fish" to 9)))

    @Test
    fun `missing ingredient means zero`() =
        assertEquals(0, CraftPlanner.maxCrafts(TestPacks.sushiRecipe(rice = 2, fish = 1), mapOf("rice" to 7)))

    @Test
    fun `ingredients naming the same item are summed`() {
        val base = TestPacks.sushiRecipe(rice = 1, fish = 1)
        val doubled = recipe("double_rice", base.station) {
            ingredient(Material.WHEAT.ref, 2)
            ingredient(Material.WHEAT.ref, 3)
            result(Material.BREAD.ref)
        }
        assertEquals(2, CraftPlanner.maxCrafts(doubled, mapOf(CraftPlanner.countKey(Material.WHEAT.ref) to 11)))
    }

    @Test
    fun `can craft needs one full set of ingredients`() {
        val sushi = TestPacks.sushiRecipe(rice = 2, fish = 1)
        assertTrue(CraftPlanner.canCraft(sushi, mapOf("rice" to 2, "fish" to 1)))
        assertFalse(CraftPlanner.canCraft(sushi, mapOf("rice" to 1, "fish" to 1)))
    }

    @Test
    fun `material and custom items have distinct count keys`() {
        val sushi = TestPacks.sushiRecipe(rice = 2, fish = 1)
        assertEquals("rice", CraftPlanner.countKey(sushi.ingredients[0].item))
        assertEquals("minecraft:wheat", CraftPlanner.countKey(Material.WHEAT.ref))
    }

    @Test
    fun `ingredients are scaled by the number of crafts`() {
        val sushi = TestPacks.sushiRecipe(rice = 2, fish = 1)
        assertEquals(
            listOf(Ingredient(sushi.ingredients[0].item, 6), Ingredient(sushi.ingredients[1].item, 3)),
            CraftPlanner.ingredientsFor(sushi, 3)
        )
    }
}
