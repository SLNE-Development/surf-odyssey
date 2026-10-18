package dev.slne.surf.odyssey.runtime.paper.hub.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/** Checks that [ShutdownSteps] runs every step and reports failures afterwards. */
class ShutdownStepsTest {
    @Test
    fun `every step runs even when an earlier one fails`() {
        val ran = mutableListOf<Int>()
        val steps = ShutdownSteps()
        steps.run { ran += 1; error("first") }
        steps.run { ran += 2 }
        steps.run { ran += 3; error("third") }
        assertEquals(listOf(1, 2, 3), ran)
    }

    @Test
    fun `the first failure is rethrown with the later ones suppressed`() {
        val first = IllegalStateException("first")
        val third = IllegalArgumentException("third")
        val steps = ShutdownSteps()
        steps.run { throw first }
        steps.run { }
        steps.run { throw third }
        val thrown = assertFailsWith<IllegalStateException> { steps.rethrow() }
        assertSame(first, thrown)
        assertEquals(listOf<Throwable>(third), thrown.suppressed.toList())
    }

    @Test
    fun `nothing is thrown when every step succeeds`() {
        val steps = ShutdownSteps()
        steps.run { }
        steps.rethrow()
    }
}
