package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks.StepSpec
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.Objective
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import org.bukkit.Material
import org.bukkit.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for the event-type index of active objectives. */
class ObjectiveIndexTest {
    /**
     * Returns the index of a quest whose step [step] has [objectives], with a stone harvest step before it for
     * every earlier step.
     */
    private fun indexOf(vararg objectives: Objective, step: Int = 0): ObjectiveIndex {
        val steps = List(step) { StepSpec(1, listOf(Objective.Harvest(Material.STONE, 1, null))) } +
                StepSpec(1, objectives.toList())
        val quest = TestPacks.questWithSteps(id = "index", steps = steps)
        return ObjectiveIndex.build(listOf(ActiveQuest(quest, step, period = 7)))
    }

    @Test
    fun `events without an indexed objective match nothing`() {
        val index = indexOf(Objective.Harvest(Material.COAL_ORE, 3, null))

        assertTrue(index.matching(ProgressEvent.Harvested(Material.IRON_ORE, null)).isEmpty())
        assertTrue(index.matching(ProgressEvent.Killed(EntityType.WOLF, null)).isEmpty())
        assertTrue(index.matching(ProgressEvent.Crafted("sushi", 1)).isEmpty())
        assertTrue(index.matching(ProgressEvent.Fished("cod", Material.COD, null)).isEmpty())
    }

    @Test
    fun `a fish event only matches fish objectives in the same region`() {
        val index = indexOf(
            Objective.Fish(null, Material.COD, 3, "harbor"),
            Objective.Fish(null, Material.COD, 3, "reef"),
            Objective.Fish(null, null, 3, null),
            Objective.Fish("golden_carp", null, 3, "harbor"),
            Objective.Harvest(Material.COD, 3, "harbor"),
        )

        val matches = index.matching(ProgressEvent.Fished("cod", Material.COD, "harbor"))

        assertEquals(listOf(0, 2), matches.map { it.objective.objective })
        assertTrue(matches.all { it.amount == 1 })
        assertEquals(listOf(2), index.matching(ProgressEvent.Fished("cod", Material.COD, null)).map { it.objective.objective })
        assertEquals(
            listOf(2, 3),
            index.matching(ProgressEvent.Fished("golden_carp", Material.TROPICAL_FISH, "harbor"))
                .map { it.objective.objective }
        )
    }

    @Test
    fun `material objectives ignore other materials`() {
        val index = indexOf(Objective.Harvest(Material.WHEAT, 5, null), Objective.Harvest(Material.WHEAT, 5, "farm"))

        assertTrue(index.matching(ProgressEvent.Harvested(Material.CARROTS, null)).isEmpty())
        assertEquals(listOf(0), index.matching(ProgressEvent.Harvested(Material.WHEAT, "mine")).map { it.objective.objective })
        assertEquals(
            listOf(0, 1),
            index.matching(ProgressEvent.Harvested(Material.WHEAT, "farm")).map { it.objective.objective }
        )
    }

    @Test
    fun `kills match the entity type and region`() {
        val index = indexOf(Objective.Kill(EntityType.ZOMBIE, 4, "crypt"), Objective.Kill(EntityType.SKELETON, 4, null))

        assertEquals(listOf(0), index.matching(ProgressEvent.Killed(EntityType.ZOMBIE, "crypt")).map { it.objective.objective })
        assertTrue(index.matching(ProgressEvent.Killed(EntityType.ZOMBIE, "field")).isEmpty())
        assertEquals(listOf(1), index.matching(ProgressEvent.Killed(EntityType.SKELETON, "field")).map { it.objective.objective })
    }

    @Test
    fun `crafts match the recipe and count the crafted amount`() {
        val recipe = ProgressionRegistry.build(TestPacks.valid()).recipe("sushi")!!
        val index = indexOf(Objective.Craft(recipe, 10), step = 1)

        val match = index.matching(ProgressEvent.Crafted("sushi", 4)).single()

        assertEquals(4, match.amount)
        assertEquals(1, match.objective.step)
        assertEquals(7, match.objective.period)
        assertTrue(index.matching(ProgressEvent.Crafted("rice_ball", 4)).isEmpty())
    }

    @Test
    fun `talk and deliver objectives are not indexed`() {
        val index = indexOf(Objective.TalkTo(TestPacks.brina, "index_offer"))

        assertTrue(index.matching(ProgressEvent.Harvested(Material.WHEAT, null)).isEmpty())
        assertTrue(index.isEmpty())
    }
}
