package dev.slne.surf.odyssey.runtime.paper.progression.operation

import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.curve.Curves
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.CurrencyReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.QuestReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.RecipeReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.TitleReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ToolReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.title.title
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.tool
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Tests for turning quest rewards into state changes, item stacks and currency payments. */
class RewardPlannerTest {
    private val pick = tool("pick", ToolType.PICKAXE) {
        name = text("Hacke")
        material = Material.IRON_PICKAXE
        curve = Curves.PICKAXE
    }

    @AfterTest
    fun uninstallTools() {
        RewardPlanner.toolRewards = null
    }

    @Test
    fun `recipe and title rewards become unlock changes`() {
        val recipe = TestPacks.valid().recipes.single()
        val angler = title("angler") { display = text("Angler") }

        val (changes, stacks) = RewardPlanner.plan(listOf(RecipeReward(recipe), TitleReward(angler)), FakeStacks::factory)

        assertEquals(listOf(StateChange.RecipeUnlocked("sushi"), StateChange.TitleUnlocked("angler")), changes)
        assertTrue(stacks.isEmpty())
    }

    @Test
    fun `item rewards become item stacks`() {
        val (changes, stacks) = RewardPlanner.plan(listOf(ItemReward(TestItems.ink, 3)), FakeStacks::factory)

        assertTrue(changes.isEmpty())
        assertEquals(listOf(TestItems.ink to 3), stacks.map(FakeStacks::contentOf))
    }

    @Test
    fun `tool rewards fail while no tool factory is installed`() {
        val error = assertFailsWith<IllegalStateException> {
            RewardPlanner.plan(listOf(ToolReward(pick)), FakeStacks::factory)
        }
        assertEquals("tool rewards are not installed", error.message)
    }

    @Test
    fun `tool rewards use the installed tool factory`() {
        val stack = FakeStacks.of(TestItems.ink, 1)
        RewardPlanner.toolRewards = ToolRewardFactory { stack }

        val (_, stacks) = RewardPlanner.plan(listOf(ToolReward(pick)), FakeStacks::factory)

        assertSame(stack, stacks.single())
    }

    @Test
    fun `currency rewards neither change state nor create items`() {
        val rewards = listOf(CurrencyReward("coins", 5.0))

        val (changes, stacks) = RewardPlanner.plan(rewards, FakeStacks::factory)

        assertTrue(changes.isEmpty())
        assertTrue(stacks.isEmpty())
        assertEquals(rewards, RewardPlanner.currencies(rewards))
    }

    @Test
    fun `unknown reward types are rejected`() {
        val unknown = object : QuestReward {
            override val typeKey: String = "mystery"
        }

        assertFailsWith<IllegalArgumentException> { RewardPlanner.plan(listOf(unknown), FakeStacks::factory) }
    }
}
