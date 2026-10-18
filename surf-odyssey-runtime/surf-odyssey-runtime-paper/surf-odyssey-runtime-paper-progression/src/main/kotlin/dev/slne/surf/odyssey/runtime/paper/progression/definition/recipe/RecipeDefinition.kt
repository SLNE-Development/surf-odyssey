package dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe

import dev.slne.surf.odyssey.runtime.paper.progression.definition.Definition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.ProgressionDsl
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import kotlin.time.Duration

/**
 * A recipe crafted at a station kind.
 *
 * @property id stable recipe ID
 * @property station the station kind the recipe is crafted at
 * @property ingredients items consumed by one craft
 * @property result the crafted item
 * @property resultAmount how many result items one craft produces
 * @property duration real time one craft takes; [Duration.ZERO] crafts instantly
 * @property unlockedByDefault whether every player knows the recipe from the start
 */
class RecipeDefinition(
    override val id: String,
    val station: StationDefinition,
    val ingredients: List<Ingredient>,
    val result: ItemRef,
    val resultAmount: Int,
    val duration: Duration,
    val unlockedByDefault: Boolean,
) : Definition {
    /** Returns [id] for log and validation messages. */
    override fun toString(): String = "RecipeDefinition($id)"
}

/**
 * Builds the values of a [RecipeDefinition].
 *
 * @property id the recipe ID
 * @property station the station kind of the recipe
 */
@ProgressionDsl
class RecipeBuilder(private val id: String, private val station: StationDefinition) {
    /** Ingredients added so far. */
    private val ingredients = mutableListOf<Ingredient>()

    /** Result item; required. */
    private var result: ItemRef? = null

    /** Result amount per craft. */
    private var resultAmount: Int = 1

    /** Real time one craft takes; zero crafts instantly. */
    var duration: Duration = Duration.ZERO

    /** Whether every player knows the recipe from the start. */
    var unlockedByDefault: Boolean = false

    /**
     * Adds an ingredient consumed by one craft.
     *
     * @param item the consumed item
     * @param amount how many are consumed
     */
    fun ingredient(item: ItemRef, amount: Int) {
        ingredients += Ingredient(item, amount)
    }

    /**
     * Sets the crafted item.
     *
     * @param item the crafted item
     * @param amount how many items one craft produces
     */
    fun result(item: ItemRef, amount: Int = 1) {
        result = item
        resultAmount = amount
    }

    /**
     * Creates the definition.
     *
     * @throws IllegalStateException if no result is set
     */
    fun build(): RecipeDefinition = RecipeDefinition(
        id = id,
        station = station,
        ingredients = ingredients.toList(),
        result = checkNotNull(result) { "recipe $id needs a result" },
        resultAmount = resultAmount,
        duration = duration,
        unlockedByDefault = unlockedByDefault,
    )
}

/**
 * Defines a recipe.
 *
 * @param id stable recipe ID
 * @param station the station kind the recipe is crafted at
 * @param block sets ingredients, result and timing
 * @return the recipe definition
 */
fun recipe(id: String, station: StationDefinition, block: RecipeBuilder.() -> Unit): RecipeDefinition =
    RecipeBuilder(id, station).apply(block).build()
