package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.CustomItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.MaterialRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import kotlin.time.Duration

/**
 * One lore line of a [RecipeIcon].
 *
 * @property text the line as plain text
 */
sealed interface RecipeLoreLine {
    val text: String

    /**
     * A line of fixed text.
     *
     * @property text the line
     * @property kind how the line is styled
     */
    data class Plain(override val text: String, val kind: LineKind) : RecipeLoreLine

    /**
     * An ingredient of the recipe with the amount one craft consumes.
     *
     * @property amount how many one craft consumes
     * @property name the name of the ingredient item
     * @property enough whether the player carries at least [amount]
     */
    data class IngredientLine(val amount: Int, val name: Component, val enough: Boolean) : RecipeLoreLine {
        /** The line as plain text, such as `2× Reis`. */
        override val text: String
            get() = "$amount× ${PlainTextComponentSerializer.plainText().serialize(name)}"
    }

    /** How a [Plain] line is styled. */
    enum class LineKind {
        /** A section heading. */
        HEADING,

        /** An empty separator line. */
        EMPTY,

        /** A value, such as the crafting time. */
        VALUE,

        /** A hint about what a click does. */
        HINT,

        /** A placeholder line of a locked recipe. */
        LOCKED
    }
}

/**
 * The description of one recipe in the recipe browser, from which the browser builds the item.
 *
 * @property name the item name as plain text
 * @property material the item material
 * @property amount the stack size shown
 * @property lore the lore lines
 * @property unlocked whether the icon shows an unlocked recipe
 */
data class RecipeIcon(
    val name: String,
    val material: Material,
    val amount: Int,
    val lore: List<RecipeLoreLine>,
    val unlocked: Boolean,
) {
    /** The lore lines as plain text. */
    val loreLines: List<String>
        get() = lore.map { it.text }

    /** Creates recipe icons. */
    companion object {
        /** The name and every lore line of a locked recipe. */
        const val LOCKED_TEXT = "???"

        /**
         * Describes a recipe the player has not unlocked: a gray dye named [LOCKED_TEXT] with one [LOCKED_TEXT] line
         * per lore line an unlocked recipe has for ingredients and duration, and nothing else.
         *
         * @param recipe the locked recipe
         */
        fun locked(recipe: RecipeDefinition): RecipeIcon = RecipeIcon(
            name = LOCKED_TEXT,
            material = Material.GRAY_DYE,
            amount = 1,
            lore = List(recipe.ingredients.size + 1) {
                RecipeLoreLine.Plain(LOCKED_TEXT, RecipeLoreLine.LineKind.LOCKED)
            },
            unlocked = false,
        )

        /**
         * Describes a recipe the player has unlocked: the result item with the result amount and a lore listing
         * every ingredient, the crafting time and the click actions. Crafting as often as possible is only offered
         * for instant recipes.
         *
         * @param recipe the unlocked recipe
         * @param inventoryCounts the carried amount of each item by [CraftPlanner.countKey]
         */
        fun unlocked(recipe: RecipeDefinition, inventoryCounts: Map<String, Int>): RecipeIcon {
            val lore = buildList {
                add(RecipeLoreLine.Plain("Zutaten:", RecipeLoreLine.LineKind.HEADING))
                recipe.ingredients.forEach { ingredient ->
                    val carried = inventoryCounts[CraftPlanner.countKey(ingredient.item)] ?: 0
                    add(RecipeLoreLine.IngredientLine(ingredient.amount, ingredient.item.displayName, carried >= ingredient.amount))
                }
                add(RecipeLoreLine.Plain("", RecipeLoreLine.LineKind.EMPTY))
                add(RecipeLoreLine.Plain(durationText(recipe.duration), RecipeLoreLine.LineKind.VALUE))
                add(RecipeLoreLine.Plain("Klick: herstellen", RecipeLoreLine.LineKind.HINT))
                if (recipe.duration == Duration.ZERO) {
                    add(RecipeLoreLine.Plain("Shift-Klick: so oft wie möglich", RecipeLoreLine.LineKind.HINT))
                }
            }
            return RecipeIcon(
                name = PlainTextComponentSerializer.plainText().serialize(recipe.result.displayName),
                material = materialOf(recipe.result),
                amount = recipe.resultAmount,
                lore = lore,
                unlocked = true,
            )
        }

        /** Returns `Sofort` for an instant recipe, otherwise `Dauer: ` and the formatted [duration]. */
        private fun durationText(duration: Duration): String =
            if (duration == Duration.ZERO) "Sofort" else "Dauer: ${DurationText.format(duration)}"

        /** Returns the base material of [ref]. */
        private fun materialOf(ref: ItemRef): Material = when (ref) {
            is CustomItemRef -> ref.item.material
            is MaterialRef -> ref.material
        }
    }
}

/** Writes durations for players, such as `1 Std 5 Min 30 s`. */
object DurationText {
    /**
     * Formats [duration] in hours, minutes and seconds, leaving out parts that are zero. Fractions of a second are
     * rounded up, so a running time is never shown as over early.
     *
     * @return the text, `0 s` for a zero or negative duration
     */
    fun format(duration: Duration): String {
        val millis = duration.inWholeMilliseconds
        if (millis <= 0) return "0 s"
        val totalSeconds = (millis + 999) / 1000
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60
        return buildList {
            if (hours > 0) add("$hours Std")
            if (minutes > 0) add("$minutes Min")
            if (seconds > 0) add("$seconds s")
        }.joinToString(" ")
    }
}
