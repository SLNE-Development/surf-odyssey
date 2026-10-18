package dev.slne.surf.odyssey.runtime.paper.progression.delivery

import dev.slne.surf.odyssey.core.common.progression.DeliveryDto
import dev.slne.surf.odyssey.runtime.paper.progression.operation.FakeCodec
import dev.slne.surf.odyssey.runtime.paper.progression.operation.FakeStacks
import dev.slne.surf.odyssey.runtime.paper.progression.operation.TestItems
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Tests how the pickup box decodes the items of the deliveries it shows. */
class DeliveryDisplayTest {
    @Test
    fun `a readable delivery shows its item`() {
        val stack = FakeStacks.of(TestItems.ink, 3)
        val delivery = DeliveryDto(UUID.randomUUID(), FakeCodec.encode(stack), "test", 0)
        val failures = mutableListOf<UUID>()

        assertSame(stack, DeliveryDisplay.decodeOrNull(FakeCodec, delivery) { id, _ -> failures += id })
        assertTrue(failures.isEmpty())
    }

    @Test
    fun `a broken delivery is reported with its id and shows no item`() {
        val delivery = DeliveryDto(UUID.randomUUID(), "corrupt", "test", 0)
        val failures = mutableListOf<UUID>()

        assertNull(DeliveryDisplay.decodeOrNull(FakeCodec, delivery) { id, _ -> failures += id })
        assertEquals(listOf(delivery.id), failures)
    }
}
