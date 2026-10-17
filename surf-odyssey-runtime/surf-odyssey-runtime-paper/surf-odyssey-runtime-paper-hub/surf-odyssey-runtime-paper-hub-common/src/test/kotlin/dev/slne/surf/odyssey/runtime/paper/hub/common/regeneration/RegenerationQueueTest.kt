package dev.slne.surf.odyssey.runtime.paper.hub.common.regeneration

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks the order and uniqueness of entries in a [RegenerationQueue]. */
class RegenerationQueueTest {
    private val a = BlockPoint(1, 2, 3)
    private val b = BlockPoint(4, 5, 6)

    @Test
    fun `entries come back once they are due, earliest first`() {
        val queue = RegenerationQueue<String>()
        queue.schedule(b, "log", dueAt = 200)
        queue.schedule(a, "ore", dueAt = 100)
        assertTrue(queue.pollDue(now = 99).isEmpty())
        assertEquals(listOf("ore", "log"), queue.pollDue(now = 200).map { it.original })
        assertEquals(0, queue.size)
    }

    @Test
    fun `a block already waiting keeps its first original`() {
        val queue = RegenerationQueue<String>()
        assertTrue(queue.schedule(a, "ore", dueAt = 100))
        assertFalse(queue.schedule(a, "stone", dueAt = 50))
        assertTrue(queue.isPending(a))
        assertEquals("ore", queue.pollDue(now = 100).single().original)
        assertFalse(queue.isPending(a))
    }

    @Test
    fun `polling at the end of time drains every entry`() {
        val queue = RegenerationQueue<String>()
        queue.schedule(a, "ore", dueAt = 100)
        queue.schedule(b, "log", dueAt = Long.MAX_VALUE)
        assertEquals(listOf("ore", "log"), queue.pollDue(now = Long.MAX_VALUE).map { it.original })
        assertEquals(0, queue.size)
    }
}
