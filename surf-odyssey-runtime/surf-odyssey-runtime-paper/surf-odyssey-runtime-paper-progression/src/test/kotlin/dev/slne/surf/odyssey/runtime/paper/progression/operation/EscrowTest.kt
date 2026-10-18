package dev.slne.surf.odyssey.runtime.paper.progression.operation

import dev.slne.surf.odyssey.core.common.progression.OperationResultDto
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.CurrencyReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Tests for escrow handling around progression operations. */
class EscrowTest {
    @Test
    fun `items are returned when the commit is rejected`() = runTest {
        val inv = FakeInventory(mapOf(TestItems.ink to 12))
        val outcome = TestRunner(commit = { OperationResultDto.Rejected("stale") })
            .run(inv, OperationPlan(listOf(Ingredient(TestItems.ink, 12)), emptyList(), emptyList(), "test"))
        assertIs<OperationOutcome.Rejected>(outcome)
        assertEquals(12, inv.count(TestItems.ink))
    }

    @Test
    fun `items dropped during the commit cannot be reused`() = runTest {
        val inv = FakeInventory(mapOf(TestItems.ink to 12))
        TestRunner(commit = { inv.dropAll(); OperationResultDto.Applied(emptyList()) })
            .run(inv, OperationPlan(listOf(Ingredient(TestItems.ink, 12)), emptyList(), emptyList(), "test"))
        assertEquals(0, inv.count(TestItems.ink))
        assertEquals(0, inv.dropped)
    }

    @Test
    fun `a second operation while one runs is busy`() = runTest {
        val inv = FakeInventory(mapOf(TestItems.ink to 24))
        val gate = CompletableDeferred<OperationResultDto>()
        val runner = TestRunner(commit = { gate.await() })
        val plan = OperationPlan(listOf(Ingredient(TestItems.ink, 12)), emptyList(), emptyList(), "test")

        val first = async { runner.run(inv, plan) }
        val second = async { runner.run(inv, plan) }
        runCurrent()
        gate.complete(OperationResultDto.Applied(emptyList()))
        val outcomes = listOf(first.await(), second.await())

        assertEquals(1, outcomes.count { it == OperationOutcome.Busy })
        assertEquals(1, outcomes.count { it == OperationOutcome.Success })
        assertEquals(12, inv.count(TestItems.ink))
    }

    @Test
    fun `overflow on a full inventory goes to the pickup box`() = runTest {
        val inv = FakeInventory(emptyMap(), freeSlots = 0)
        val runner = TestRunner(commit = { OperationResultDto.Applied(listOf(UUID.randomUUID())) })
        runner.run(inv, OperationPlan(emptyList(), emptyList(), listOf(ItemReward(TestItems.ink, 3)), "test"))
        assertEquals(1, runner.unclaimedDeliveries)
        assertEquals(1, runner.pickupNotices)
    }

    @Test
    fun `item rewards go into the inventory when they fit`() = runTest {
        val inv = FakeInventory(emptyMap())
        val runner = TestRunner(commit = { OperationResultDto.Applied(listOf(UUID.randomUUID())) })

        val outcome = runner.run(inv, OperationPlan(emptyList(), emptyList(), listOf(ItemReward(TestItems.ink, 3)), "t"))

        assertEquals(OperationOutcome.Success, outcome)
        assertEquals(3, inv.count(TestItems.ink))
        assertEquals(0, runner.unclaimedDeliveries)
        assertEquals(0, runner.pickupNotices)
    }

    @Test
    fun `missing items take nothing and commit nothing`() = runTest {
        val inv = FakeInventory(mapOf(TestItems.ink to 5))
        var commits = 0
        val runner = TestRunner(commit = { commits++; OperationResultDto.Applied(emptyList()) })

        val outcome = runner.run(inv, OperationPlan(listOf(Ingredient(TestItems.ink, 12)), emptyList(), emptyList(), "t"))

        assertEquals(OperationOutcome.MissingItems, outcome)
        assertEquals(5, inv.count(TestItems.ink))
        assertEquals(0, commits)
    }

    @Test
    fun `a failing commit returns the items and reports the service as unavailable`() = runTest {
        val inv = FakeInventory(mapOf(TestItems.ink to 12))
        val runner = TestRunner(commit = { error("service down") })

        val outcome = runner.run(inv, OperationPlan(listOf(Ingredient(TestItems.ink, 12)), emptyList(), emptyList(), "t"))

        assertEquals(OperationOutcome.Unavailable, outcome)
        assertEquals(12, inv.count(TestItems.ink))
        assertTrue(runner.serviceFailures > 0)
    }

    @Test
    fun `a commit that times out returns the items`() = runTest {
        val inv = FakeInventory(mapOf(TestItems.ink to 12))
        val runner = TestRunner(commit = { awaitCancellation() })

        val outcome = runner.run(inv, OperationPlan(listOf(Ingredient(TestItems.ink, 12)), emptyList(), emptyList(), "t"))

        assertEquals(OperationOutcome.Unavailable, outcome)
        assertEquals(12, inv.count(TestItems.ink))
    }

    @Test
    fun `returned items that do not fit go to the pickup box`() = runTest {
        val inv = FakeInventory(mapOf(TestItems.ink to 12), freeSlots = 0)
        var calls = 0
        val runner = TestRunner(commit = {
            if (calls++ == 0) OperationResultDto.Rejected("stale") else OperationResultDto.Applied(listOf(UUID.randomUUID()))
        })

        runner.run(inv, OperationPlan(listOf(Ingredient(TestItems.ink, 12)), emptyList(), emptyList(), "t"))

        assertEquals(1, runner.unclaimedDeliveries)
        assertEquals(1, runner.pickupNotices)
        assertTrue(runner.droppedAtFeet.isEmpty())
    }

    @Test
    fun `returned items are dropped at the feet when the pickup box fails too`() = runTest {
        val inv = FakeInventory(mapOf(TestItems.ink to 12), freeSlots = 0)
        val runner = TestRunner(commit = { error("service down") })

        runner.run(inv, OperationPlan(listOf(Ingredient(TestItems.ink, 12)), emptyList(), emptyList(), "t"))

        assertEquals(1, runner.droppedAtFeet.size)
        assertEquals(0, runner.unclaimedDeliveries)
    }

    @Test
    fun `deliveries that cannot be claimed stay in the pickup box`() = runTest {
        val inv = FakeInventory(emptyMap())
        val runner = TestRunner(commit = { OperationResultDto.Applied(listOf(UUID.randomUUID())) }, claimFails = true)

        val outcome = runner.run(inv, OperationPlan(emptyList(), emptyList(), listOf(ItemReward(TestItems.ink, 3)), "t"))

        assertEquals(OperationOutcome.Success, outcome)
        assertEquals(1, runner.unclaimedDeliveries)
        assertEquals(1, runner.pickupNotices)
    }

    @Test
    fun `applied operations update the cache and pay currencies`() = runTest {
        val inv = FakeInventory(emptyMap())
        val runner = TestRunner(commit = { OperationResultDto.Applied(emptyList()) })
        val change = StateChange.QuestCompleted("q", fromStep = 0)

        runner.run(inv, OperationPlan(emptyList(), listOf(change), listOf(CurrencyReward("coins", 5.0)), "t"))

        assertEquals(listOf(listOf<StateChange>(change)), runner.appliedChanges)
        assertEquals(listOf("coins" to 5.0), runner.grants)
    }

    @Test
    fun `rejected operations neither update the cache nor pay currencies`() = runTest {
        val inv = FakeInventory(emptyMap())
        val runner = TestRunner(commit = { OperationResultDto.Rejected("stale") })

        runner.run(inv, OperationPlan(emptyList(), emptyList(), listOf(CurrencyReward("coins", 5.0)), "t"))

        assertTrue(runner.appliedChanges.isEmpty())
        assertTrue(runner.grants.isEmpty())
    }
}
