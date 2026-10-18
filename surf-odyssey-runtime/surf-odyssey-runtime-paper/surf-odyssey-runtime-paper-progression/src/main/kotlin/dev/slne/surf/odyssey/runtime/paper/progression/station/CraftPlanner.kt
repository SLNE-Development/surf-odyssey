package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.core.common.progression.StationJobDto
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.CustomItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.MaterialRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationPlan
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Crafting arithmetic of recipes: how often a recipe can be crafted from the carried items, which items a number of
 * crafts consumes, and the operations that craft, start and collect station jobs.
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

    /**
     * Returns how many crafts one click makes: one, or with [everything] as many as [maxCrafts] allows, at most
     * [MAX_CRAFTS_PER_CLICK]. At least one craft is returned, so missing ingredients are reported by the operation.
     *
     * @param maxCrafts how often the recipe can be crafted from the carried items
     * @param everything whether the click crafts as often as possible
     */
    fun timesFor(maxCrafts: Int, everything: Boolean): Int =
        if (everything) maxCrafts.coerceIn(1, MAX_CRAFTS_PER_CLICK) else 1

    /**
     * Plans [times] instant crafts of [recipe]: the ingredients of every craft are taken and the results are given.
     *
     * @param recipe the crafted recipe
     * @param times how many crafts are made
     */
    fun craftPlan(recipe: RecipeDefinition, times: Int): OperationPlan = OperationPlan(
        take = ingredientsFor(recipe, times),
        changes = emptyList(),
        rewards = listOf(ItemReward(recipe.result, recipe.resultAmount * times)),
        source = CRAFT_SOURCE,
    )

    /**
     * Plans starting a job of one craft of the timed [recipe]: one ingredient set is taken and a job of the recipe's
     * station kind is stored that is ready after the recipe's duration.
     *
     * @param recipe the crafted recipe
     * @param jobId the id of the new job
     * @param now the start time of the job
     */
    fun jobPlan(recipe: RecipeDefinition, jobId: UUID, now: Instant): OperationPlan {
        val startedAt = now.toEpochMilli()
        val job = StationJobDto(
            id = jobId,
            stationType = recipe.station.id,
            recipeId = recipe.id,
            amount = 1,
            startedAt = startedAt,
            readyAt = startedAt + recipe.duration.inWholeMilliseconds,
        )
        return OperationPlan(
            take = ingredientsFor(recipe, 1),
            changes = listOf(StateChange.JobStarted(job)),
            rewards = emptyList(),
            source = STATION_SOURCE,
        )
    }

    /**
     * Plans collecting [job] of [recipe]: the job is marked collected and the results of all its crafts are given.
     *
     * @param recipe the recipe of the job
     * @param job the collected job
     */
    fun collectPlan(recipe: RecipeDefinition, job: StationJobDto): OperationPlan = OperationPlan(
        take = emptyList(),
        changes = listOf(StateChange.JobCollected(job.id)),
        rewards = listOf(ItemReward(recipe.result, recipe.resultAmount * job.amount)),
        source = STATION_SOURCE,
    )

    /** Returns whether [job] is done at [now]. */
    fun isReady(job: StationJobDto, now: Instant): Boolean = now.toEpochMilli() >= job.readyAt

    /** Returns how long [job] still runs at [now], zero once it is ready. */
    fun remaining(job: StationJobDto, now: Instant): Duration =
        (job.readyAt - now.toEpochMilli()).coerceAtLeast(0).milliseconds

    /** The most crafts a single click makes. */
    const val MAX_CRAFTS_PER_CLICK = 64

    /** The delivery source of instant crafts. */
    private const val CRAFT_SOURCE = "craft"

    /** The delivery source of station jobs. */
    private const val STATION_SOURCE = "station"

    /** Returns the amount one craft of [recipe] consumes of each item, by [countKey]. */
    private fun neededPerCraft(recipe: RecipeDefinition): Map<String, Int> =
        recipe.ingredients.groupingBy { countKey(it.item) }.fold(0) { sum, ingredient -> sum + ingredient.amount }
}
