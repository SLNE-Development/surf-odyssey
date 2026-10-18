package dev.slne.surf.odyssey.runtime.paper.hub.common.progression

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests when the double drop bonus of a tool doubles a harvest or catch and how doubled blocks are remembered. */
class DoubleDropTest {
    @Test
    fun `a roll below the chance doubles the drops`() {
        assertEquals(2, DoubleDrop.times(chance = 0.25, roll = 0.1))
    }

    @Test
    fun `a roll at or above the chance keeps single drops`() {
        assertEquals(1, DoubleDrop.times(chance = 0.25, roll = 0.25))
        assertEquals(1, DoubleDrop.times(chance = 0.25, roll = 0.9))
    }

    @Test
    fun `a tool without the bonus never doubles`() {
        assertEquals(1, DoubleDrop.times(chance = 0.0, roll = 0.0))
    }

    @Test
    fun `a marked block is doubled once`() {
        val pending = PendingDoubleDrops()
        val point = BlockPoint(1, 64, 2)
        pending.mark(point)
        assertTrue(pending.take(point))
        assertFalse(pending.take(point))
    }

    @Test
    fun `a forgotten or unmarked block is not doubled`() {
        val pending = PendingDoubleDrops()
        val point = BlockPoint(1, 64, 2)
        pending.mark(point)
        pending.forget(point)
        assertFalse(pending.take(point))
        assertFalse(pending.take(BlockPoint(0, 0, 0)))
    }
}
