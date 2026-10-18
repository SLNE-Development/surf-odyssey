package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.recipe
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.core.common.progression.StationJobDto
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import org.bukkit.Material
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

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

    @Test
    fun `an instant craft takes the ingredients and rewards the results of every craft`() {
        val sushi = TestPacks.sushiRecipe(rice = 2, fish = 1)
        val plan = CraftPlanner.craftPlan(sushi, 4)
        assertEquals(CraftPlanner.ingredientsFor(sushi, 4), plan.take)
        assertEquals(emptyList(), plan.changes)
        assertEquals(listOf(ItemReward(sushi.result, sushi.resultAmount * 4)), plan.rewards)
        assertTrue(plan.source.length <= 64)
    }

    @Test
    fun `a timed craft starts one job with one set of ingredients`() {
        val sushi = TestPacks.sushiRecipe(rice = 2, fish = 1, duration = 90.seconds)
        val id = UUID.randomUUID()
        val now = Instant.ofEpochMilli(1_000_000)
        val plan = CraftPlanner.jobPlan(sushi, id, now)
        assertEquals(CraftPlanner.ingredientsFor(sushi, 1), plan.take)
        assertEquals(
            listOf(StateChange.JobStarted(StationJobDto(id, "sushi_bar", "sushi", 1, 1_000_000, 1_090_000))),
            plan.changes
        )
        assertEquals(emptyList(), plan.rewards)
        assertEquals("station", plan.source)
    }

    @Test
    fun `collecting a job marks it collected and rewards its results`() {
        val sushi = TestPacks.sushiRecipe(rice = 2, fish = 1, duration = 90.seconds)
        val job = StationJobDto(UUID.randomUUID(), "sushi_bar", "sushi", 2, 0, 90_000)
        val plan = CraftPlanner.collectPlan(sushi, job)
        assertEquals(emptyList(), plan.take)
        assertEquals(listOf(StateChange.JobCollected(job.id)), plan.changes)
        assertEquals(listOf(ItemReward(sushi.result, sushi.resultAmount * 2)), plan.rewards)
        assertEquals("station", plan.source)
    }

    @Test
    fun `a job is ready from its ready time on`() {
        val job = StationJobDto(UUID.randomUUID(), "sushi_bar", "sushi", 1, 0, 90_000)
        assertFalse(CraftPlanner.isReady(job, Instant.ofEpochMilli(89_999)))
        assertTrue(CraftPlanner.isReady(job, Instant.ofEpochMilli(90_000)))
        assertEquals(30.seconds, CraftPlanner.remaining(job, Instant.ofEpochMilli(60_000)))
        assertEquals(0.seconds, CraftPlanner.remaining(job, Instant.ofEpochMilli(95_000)))
    }

    @Test
    fun `a shift click crafts as often as possible up to a stack`() {
        assertEquals(1, CraftPlanner.timesFor(maxCrafts = 9, everything = false))
        assertEquals(9, CraftPlanner.timesFor(maxCrafts = 9, everything = true))
        assertEquals(64, CraftPlanner.timesFor(maxCrafts = 200, everything = true))
        assertEquals(1, CraftPlanner.timesFor(maxCrafts = 0, everything = true))
    }
}
