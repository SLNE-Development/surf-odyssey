package dev.slne.surf.odyssey.runtime.paper.progression.tool

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests for buffering tool usage in memory and deciding when it is written to the item. */
class ToolWriteBufferTest {
    /** The buffered tool. */
    private val tool = UUID.randomUUID()

    /** The player holding [tool]. */
    private val holder = UUID.randomUUID()

    /** A buffer that writes at most once per second. */
    private val buffer = ToolWriteBuffer(intervalMillis = 1_000)

    @Test
    fun `a tool that was never written is written at once`() {
        buffer.add(tool, holder, 5, null)

        assertTrue(buffer.shouldWriteNow(tool, now = 0, leveledUp = false))
    }

    @Test
    fun `a tool written less than a second ago waits`() {
        buffer.add(tool, holder, 5, null)
        buffer.drain(tool, now = 10_000)
        buffer.add(tool, holder, 5, null)

        assertFalse(buffer.shouldWriteNow(tool, now = 10_999, leveledUp = false))
        assertTrue(buffer.shouldWriteNow(tool, now = 11_000, leveledUp = false))
    }

    @Test
    fun `a level-up is written at once`() {
        buffer.drain(tool, now = 10_000)
        buffer.add(tool, holder, 5, null)

        assertTrue(buffer.shouldWriteNow(tool, now = 10_001, leveledUp = true))
    }

    @Test
    fun `buffered usage and events add up until they are drained`() {
        buffer.add(tool, holder, 5, ChronicleEvent.BlockMined)
        buffer.add(tool, holder, 3, null)
        buffer.add(tool, holder, 2, ChronicleEvent.MobKilled)

        assertEquals(10, buffer.pendingUsage(tool))
        val delta = buffer.drain(tool, now = 0)
        assertEquals(ToolWriteBuffer.Delta(10, listOf(ChronicleEvent.BlockMined, ChronicleEvent.MobKilled)), delta)
        assertEquals(0, buffer.pendingUsage(tool))
        assertNull(buffer.drain(tool, now = 0))
    }

    @Test
    fun `only buffered tools whose interval passed are due`() {
        val other = UUID.randomUUID()
        buffer.drain(tool, now = 10_000)
        buffer.drain(other, now = 10_500)
        buffer.add(tool, holder, 1, null)
        buffer.add(other, holder, 1, null)

        assertEquals(listOf(tool to holder), buffer.due(now = 11_200))
    }
}
