package dev.slne.surf.odyssey.runtime.paper.progression.definition

import dev.slne.surf.odyssey.runtime.paper.progression.definition.curve.Curves
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for tool level curves. */
class CurvesTest {
    @Test
    fun `quadratic matches the documented table`() {
        val c = Curves.quadratic(100_000)
        assertEquals(0, c.usageFor(1))
        assertEquals(11_111, c.usageFor(4))
        assertEquals(100_000, c.usageFor(10))
    }

    @Test
    fun `level for usage is the highest reached level`() {
        assertEquals(3, Curves.levelFor(Curves.quadratic(100_000), 5_000, 10))
        assertEquals(10, Curves.levelFor(Curves.quadratic(100_000), 999_999, 10))
    }

    @Test
    fun `predefined curves are strictly increasing`() {
        listOf(Curves.PICKAXE, Curves.AXE, Curves.ROD, Curves.WEAPON).forEach { c ->
            (2..10).forEach { assertTrue(c.usageFor(it) > c.usageFor(it - 1)) }
        }
    }
}
