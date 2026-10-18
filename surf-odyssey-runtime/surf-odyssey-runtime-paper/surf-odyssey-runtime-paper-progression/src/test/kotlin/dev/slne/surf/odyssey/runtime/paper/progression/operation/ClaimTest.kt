package dev.slne.surf.odyssey.runtime.paper.progression.operation

import dev.slne.surf.odyssey.core.common.progression.DeliveryDto
import dev.slne.surf.odyssey.core.common.progression.OperationResultDto
import dev.slne.surf.odyssey.runtime.paper.progression.delivery.ClaimResult
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for claiming single deliveries from the pickup box. */
@OptIn(ExperimentalCoroutinesApi::class)
class ClaimTest {
    private fun delivery() = DeliveryDto(UUID.randomUUID(), FakeCodec.encode(FakeStacks.of(TestItems.ink, 3)), "t", 0)

    @Test
    fun `a claim while an operation of the player runs is busy`() = runTest {
        val inv = FakeInventory(mapOf(TestItems.ink to 12))
        val gate = CompletableDeferred<OperationResultDto>()
        val runner = TestRunner(commit = { gate.await() })

        val operation = async {
            runner.run(inv, OperationPlan(listOf(Ingredient(TestItems.ink, 12)), emptyList(), emptyList(), "t"))
        }
        runCurrent()
        val claim = runner.claim(inv, delivery())
        gate.complete(OperationResultDto.Applied(emptyList()))
        operation.await()

        assertEquals(ClaimResult.BUSY, claim)
    }

    @Test
    fun `a delivery without room in the inventory is not claimed`() = runTest {
        val runner = TestRunner(commit = { OperationResultDto.Applied(emptyList()) }, claimFails = true)

        assertEquals(ClaimResult.NO_ROOM, runner.claim(FakeInventory(emptyMap()), delivery(), fits = { false }))
    }

    @Test
    fun `a delivery whose item cannot be decoded is not claimed`() = runTest {
        val runner = TestRunner(commit = { OperationResultDto.Applied(emptyList()) }, claimFails = true)
        val broken = DeliveryDto(UUID.randomUUID(), "corrupt", "t", 0)

        assertEquals(ClaimResult.BROKEN, runner.claim(FakeInventory(emptyMap()), broken))
        assertEquals(listOf(broken.id), runner.brokenDeliveries)
    }
}
