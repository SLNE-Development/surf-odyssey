package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockBoxTest {
    private val box = BlockBox.of(BlockPos(10, 5, -3), BlockPos(0, 0, 3))

    @Test
    fun `normalizes corners`() {
        assertEquals(BlockBox(0, 0, -3, 10, 5, 3), box)
    }

    @Test
    fun `contains is inclusive on both ends`() {
        assertTrue(box.contains(0, 0, -3))
        assertTrue(box.contains(10, 5, 3))
        assertFalse(box.contains(11, 5, 3))
        assertFalse(box.contains(0, -1, 0))
    }

    @Test
    fun `double positions use the block they are in`() {
        assertTrue(box.contains(10.99, 5.5, 3.2))
        assertFalse(box.contains(-0.01, 1.0, 0.0))
    }

    @Test
    fun `volume counts blocks`() {
        assertEquals(11L * 6L * 7L, box.volume)
    }
}
