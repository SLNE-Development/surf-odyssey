package dev.slne.surf.odyssey.runtime.paper.polartest.command

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks that [StepGate] lets only one test run at a time. */
class StepGateTest {
    @Test
    fun `a second run is refused until the first one left`() {
        val gate = StepGate()
        assertTrue(gate.tryEnter())
        assertFalse(gate.tryEnter())
        gate.leave()
        assertTrue(gate.tryEnter())
    }
}
