package dev.slne.surf.odyssey.runtime.paper.progression.tool

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests for buffering tool usage per holder in memory and deciding when it is written to the item. */
class ToolWriteBufferTest {
    /** The buffered tool. */
    private val tool = UUID.randomUUID()

    /** The player holding [tool]. */
    private val holder = UUID.randomUUID()

    /** A buffer that writes at most once per second. */
    private val buffer = ToolWriteBuffer(intervalMillis = 1_000)

    @Test
    fun `a tool that was never written is written at once`() {
        buffer.add(holder, tool, slot = 0, usage = 5, event = null)

        assertTrue(buffer.shouldWriteNow(holder, tool, now = 0, leveledUp = false))
    }

    @Test
    fun `a tool written less than a second ago waits`() {
        buffer.add(holder, tool, 0, 5, null)
        buffer.drain(holder, tool, now = 10_000)
        buffer.add(holder, tool, 0, 5, null)

        assertFalse(buffer.shouldWriteNow(holder, tool, now = 10_999, leveledUp = false))
        assertTrue(buffer.shouldWriteNow(holder, tool, now = 11_000, leveledUp = false))
    }

    @Test
    fun `a level-up is written at once`() {
        buffer.drain(holder, tool, now = 10_000)
        buffer.add(holder, tool, 0, 5, null)

        assertTrue(buffer.shouldWriteNow(holder, tool, now = 10_001, leveledUp = true))
    }

    @Test
    fun `buffered usage and events add up until they are drained`() {
        buffer.add(holder, tool, 2, 5, ChronicleEvent.BlockMined)
        buffer.add(holder, tool, 2, 3, null)
        buffer.add(holder, tool, 4, 2, ChronicleEvent.MobKilled)

        assertEquals(10, buffer.pendingUsage(holder, tool))
        assertEquals(4, buffer.slotOf(holder, tool))
        val delta = buffer.drain(holder, tool, now = 0)
        assertEquals(ToolWriteBuffer.Delta(10, listOf(ChronicleEvent.BlockMined, ChronicleEvent.MobKilled)), delta)
        assertEquals(0, buffer.pendingUsage(holder, tool))
        assertNull(buffer.drain(holder, tool, now = 0))
    }

    @Test
    fun `two holders of the same tool id are buffered and written apart`() {
        val second = UUID.randomUUID()
        buffer.add(holder, tool, 0, 5, ChronicleEvent.BlockMined)
        buffer.add(second, tool, 3, 7, null)

        assertEquals(5, buffer.pendingUsage(holder, tool))
        assertEquals(7, buffer.pendingUsage(second, tool))
        assertEquals(ToolWriteBuffer.Delta(5, listOf(ChronicleEvent.BlockMined)), buffer.drain(holder, tool, now = 10_000))

        assertTrue(buffer.hasPending(second, tool))
        assertEquals(3, buffer.slotOf(second, tool))
        assertTrue(buffer.shouldWriteNow(second, tool, now = 10_001, leveledUp = false))
        assertEquals(ToolWriteBuffer.Delta(7, emptyList()), buffer.drain(second, tool, now = 10_001))
    }

    @Test
    fun `only buffered entries whose interval passed are due`() {
        val other = UUID.randomUUID()
        buffer.drain(holder, tool, now = 10_000)
        buffer.drain(holder, other, now = 10_500)
        buffer.add(holder, tool, 0, 1, null)
        buffer.add(holder, other, 1, 1, null)

        assertEquals(listOf(holder to tool), buffer.due(now = 11_200))
    }

    @Test
    fun `the pending tools of a holder are listed and discarded`() {
        val other = UUID.randomUUID()
        buffer.add(holder, tool, 0, 1, null)
        buffer.add(UUID.randomUUID(), other, 0, 1, null)

        assertEquals(listOf(tool), buffer.pendingOf(holder))
        buffer.discard(holder)
        assertEquals(emptyList(), buffer.pendingOf(holder))
        assertTrue(buffer.shouldWriteNow(holder, tool, now = 0, leveledUp = false))
    }
}
