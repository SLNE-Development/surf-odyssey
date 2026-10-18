package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.core.common.progression.QuestStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks.StepSpec
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.Objective
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.CurrencyReward
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests for the operation plans of quest transitions. */
class QuestPlansTest {
    private val koch = TestPacks.npcNamed("koch")
    private val stepReward = CurrencyReward("shells", 5.0)
    private val questReward = CurrencyReward("shells", 50.0)
    private val quest = TestPacks.questWithSteps(
        id = "delivery",
        steps = listOf(
            StepSpec(
                4, listOf(
                    Objective.Deliver(Material.COD.ref, 10, TestPacks.brina),
                    Objective.Deliver(Material.SALMON.ref, 2, koch),
                    Objective.TalkTo(TestPacks.brina, "delivery_offer"),
                ), listOf(stepReward)
            ),
            StepSpec(2, listOf(Objective.Harvest(Material.WHEAT, 3, null)), listOf(stepReward)),
        ),
        rewards = listOf(questReward),
    )

    private fun state(step: Int, period: Long = 0) = QuestStateDto(quest.id, QuestStatus.ACTIVE, step, 1, period, null)

    @Test
    fun `accepting starts at the first step version and the given period`() {
        val plan = QuestPlans.accept(quest, period = 9)

        assertEquals(listOf<StateChange>(StateChange.QuestAccepted(quest.id, 4, 9)), plan.changes)
        assertTrue(plan.take.isEmpty())
        assertTrue(plan.rewards.isEmpty())
    }

    @Test
    fun `delivering takes only the missing items and sets the full count`() {
        val plan = assertNotNull(
            QuestPlans.deliver(quest, state(0, period = 3), QuestProgress(0, listOf(4, 0, 0), false), TestPacks.brina)
        )

        assertEquals(listOf(Ingredient(Material.COD.ref, 6)), plan.take)
        assertEquals(listOf<StateChange>(StateChange.CounterSet(quest.id, 0, 4, 0, 3, 10)), plan.changes)
    }

    @Test
    fun `delivering with nothing missing at this npc plans nothing`() {
        assertNull(QuestPlans.deliver(quest, state(0), QuestProgress(0, listOf(10, 0, 0), false), TestPacks.brina))
        assertNull(QuestPlans.deliver(quest, state(1), QuestProgress(1, listOf(0), false), TestPacks.brina))
    }

    @Test
    fun `talking completes the open talk objectives`() {
        val plan = assertNotNull(QuestPlans.completeTalk(quest, state(0), QuestProgress(0, listOf(0, 0, 0), false), TestPacks.brina))

        assertEquals(listOf<StateChange>(StateChange.CounterSet(quest.id, 0, 4, 2, 0, 1)), plan.changes)
        assertNull(QuestPlans.completeTalk(quest, state(0), QuestProgress(0, listOf(0, 0, 1), false), TestPacks.brina))
    }

    @Test
    fun `turning in a step advances with the step rewards`() {
        val plan = assertNotNull(QuestPlans.turnIn(quest, state(0), QuestProgress(0, listOf(10, 2, 1), true), TestPacks.brina))

        assertEquals(listOf<StateChange>(StateChange.QuestStepAdvanced(quest.id, 0, 1, 2)), plan.changes)
        assertEquals(listOf(stepReward), plan.rewards)
    }

    @Test
    fun `turning in the last step completes with step and quest rewards`() {
        val plan = assertNotNull(QuestPlans.turnIn(quest, state(1), QuestProgress(1, listOf(3), true), TestPacks.brina))

        assertEquals(listOf<StateChange>(StateChange.QuestCompleted(quest.id, 1)), plan.changes)
        assertEquals(listOf(stepReward, questReward), plan.rewards)
    }

    @Test
    fun `turning in with open objectives plans nothing`() {
        assertNull(QuestPlans.turnIn(quest, state(1), QuestProgress(1, listOf(2), false), TestPacks.brina))
    }

    @Test
    fun `talking at another npc plans nothing`() {
        assertNull(QuestPlans.completeTalk(quest, state(0), QuestProgress(0, listOf(0, 0, 0), false), koch))
    }

    @Test
    fun `turning in at another npc than the step's plans nothing`() {
        assertNull(QuestPlans.turnIn(quest, state(1), QuestProgress(1, listOf(3), true), koch))
    }
}
