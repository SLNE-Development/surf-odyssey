package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Tests for the recipe browser icons. */
class RecipeIconTest {
    @Test
    fun `locked recipes reveal nothing`() {
        val icon = RecipeIcon.locked(TestPacks.sushiRecipe(rice = 2, fish = 1))
        assertEquals(Material.GRAY_DYE, icon.material)
        assertTrue(icon.loreLines.all { it == "???" })
        assertEquals("???", icon.name)
        assertEquals(3, icon.loreLines.size)
        assertEquals(1, icon.amount)
    }

    @Test
    fun `unlocked recipes list ingredients and duration`() {
        val recipe = TestPacks.sushiRecipe(rice = 2, fish = 1, duration = 90.seconds)
        val icon = RecipeIcon.unlocked(recipe, mapOf("rice" to 5))
        assertEquals("sushi", icon.name)
        assertEquals(Material.COOKED_COD, icon.material)
        assertEquals(
            listOf("Zutaten:", "2× rice", "1× fish", "", "Dauer: 1 Min 30 s", "Klick: herstellen"),
            icon.loreLines
        )
    }

    @Test
    fun `ingredient lines show whether enough is carried`() {
        val icon = RecipeIcon.unlocked(TestPacks.sushiRecipe(rice = 2, fish = 1), mapOf("rice" to 2))
        val ingredients = icon.lore.filterIsInstance<RecipeLoreLine.IngredientLine>()
        assertEquals(listOf(true, false), ingredients.map { it.enough })
    }

    @Test
    fun `instant recipes offer crafting as often as possible`() {
        val recipe = TestPacks.sushiRecipe(rice = 2, fish = 1)
        val icon = RecipeIcon.unlocked(recipe, emptyMap())
        assertEquals(
            listOf("Zutaten:", "2× rice", "1× fish", "", "Sofort", "Klick: herstellen", "Shift-Klick: so oft wie möglich"),
            icon.loreLines
        )
    }

    @Test
    fun `unlocked icons show the result amount`() {
        val base = TestPacks.sushiRecipe(rice = 2, fish = 1)
        val icon = RecipeIcon.unlocked(base, emptyMap())
        assertEquals(base.resultAmount, icon.amount)
    }

    @Test
    fun `durations are written with their non zero parts`() {
        assertEquals("1 Min 30 s", DurationText.format(90.seconds))
        assertEquals("2 Min", DurationText.format(2.minutes))
        assertEquals("1 Std 5 s", DurationText.format(1.hours + 5.seconds))
        assertEquals("1 s", DurationText.format(200.seconds / 1000))
        assertEquals("0 s", DurationText.format(0.seconds))
    }

    @Test
    fun `a locked icon is not unlocked`() {
        assertFalse(RecipeIcon.locked(TestPacks.sushiRecipe(rice = 1, fish = 1)).unlocked)
        assertTrue(RecipeIcon.unlocked(TestPacks.sushiRecipe(rice = 1, fish = 1), emptyMap()).unlocked)
    }
}
