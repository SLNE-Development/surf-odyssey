package dev.slne.surf.odyssey.runtime.paper.polartest.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Checks that [TestLayout] keeps every test block inside its chunk and never puts two blocks on one spot. */
class TestLayoutTest {
    @Test
    fun `every block lies inside its chunk on the block row`() {
        for ((cx, cz) in TestLayout.chunks) {
            for (pos in TestLayout.cobblestone(cx, cz) + TestLayout.mossy(cx, cz)) {
                assertEquals(cx, Math.floorDiv(pos.x, 16))
                assertEquals(cz, Math.floorDiv(pos.z, 16))
                assertEquals(TestLayout.BLOCK_Y, pos.y)
            }
        }
    }

    @Test
    fun `cobblestone and mossy cobblestone never share a spot`() {
        val all = TestLayout.chunks.flatMap { (cx, cz) -> TestLayout.cobblestone(cx, cz) + TestLayout.mossy(cx, cz) }
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun `the block rows stay below the top of the live migration scan`() {
        assertTrue(TestLayout.BLOCK_Y in TestLayout.FLOOR_Y..TestLayout.TOP_Y)
    }
}
