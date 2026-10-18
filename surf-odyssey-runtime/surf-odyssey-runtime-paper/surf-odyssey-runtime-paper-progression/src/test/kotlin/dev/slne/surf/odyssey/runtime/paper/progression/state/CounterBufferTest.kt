package dev.slne.surf.odyssey.runtime.paper.progression.state

import dev.slne.surf.odyssey.core.common.progression.CounterDeltaDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for summing, draining and re-queueing buffered counter deltas. */
class CounterBufferTest {
    /** Creates a delta of [amount] for objective [objective] of step 0, version 1, period 0 of [questId]. */
    private fun delta(objective: Int, amount: Int, questId: String = "q") =
        CounterDeltaDto(questId, step = 0, stepVersion = 1, objective = objective, period = 0, delta = amount)

    @Test
    fun `deltas of the same counter are summed`() {
        val buffer = CounterBuffer()
        buffer.add(delta(0, 2))
        buffer.add(delta(0, 3))
        buffer.add(delta(1, 1))

        assertEquals(setOf(delta(0, 5), delta(1, 1)), buffer.drain().toSet())
    }

    @Test
    fun `drain returns the deltas and empties the buffer`() {
        val buffer = CounterBuffer()
        buffer.add(delta(0, 4))

        assertEquals(listOf(delta(0, 4)), buffer.drain())
        assertTrue(buffer.drain().isEmpty())
        assertTrue(buffer.isEmpty())
    }

    @Test
    fun `deltas of a failed flush are queued again and summed with new ones`() {
        val buffer = CounterBuffer()
        buffer.add(delta(0, 4))
        val failed = buffer.drain()
        buffer.add(delta(0, 1))

        buffer.requeue(failed)

        assertEquals(listOf(delta(0, 5)), buffer.drain())
    }

    @Test
    fun `different quests are kept apart`() {
        val buffer = CounterBuffer()
        buffer.add(delta(0, 1, questId = "a"))
        buffer.add(delta(0, 1, questId = "b"))

        assertEquals(2, buffer.drain().size)
    }

    @Test
    fun `pending returns the deltas without removing them`() {
        val buffer = CounterBuffer()
        buffer.add(delta(0, 2))

        assertEquals(listOf(delta(0, 2)), buffer.pending())
        assertEquals(listOf(delta(0, 2)), buffer.drain())
    }
}
