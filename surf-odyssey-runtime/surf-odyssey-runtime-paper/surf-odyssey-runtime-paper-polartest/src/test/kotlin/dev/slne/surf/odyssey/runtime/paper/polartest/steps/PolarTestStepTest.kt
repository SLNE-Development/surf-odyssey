package dev.slne.surf.odyssey.runtime.paper.polartest.steps

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Checks the command ids of the test steps and the order `/polartest run` uses. */
class PolarTestStepTest {
    @Test
    fun `every step has its own id`() {
        val ids = PolarTestStep.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `run starts with setup and never deletes the world`() {
        assertEquals(PolarTestStep.SETUP, PolarTestStep.runOrder.first())
        assertFalse(PolarTestStep.CLEANUP in PolarTestStep.runOrder)
        assertEquals(
            listOf("setup", "handler", "migrate-raw", "migrate-live", "furniture", "fail-newer", "fail-throw"),
            PolarTestStep.runOrder.map { it.id }
        )
    }
}
