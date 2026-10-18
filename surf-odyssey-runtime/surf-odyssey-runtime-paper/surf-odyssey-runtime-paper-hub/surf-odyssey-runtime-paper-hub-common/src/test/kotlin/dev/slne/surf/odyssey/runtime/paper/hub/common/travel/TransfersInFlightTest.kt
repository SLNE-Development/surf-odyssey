package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests that [TransfersInFlight] allows one running transfer per player. */
class TransfersInFlightTest {
    @Test
    fun `a second transfer cannot start while the first is running`() {
        val inFlight = TransfersInFlight()
        val id = UUID.randomUUID()
        assertTrue(inFlight.tryStart(id))
        assertTrue(inFlight.isRunning(id))
        assertFalse(inFlight.tryStart(id))
    }

    @Test
    fun `a finished transfer lets the next one start`() {
        val inFlight = TransfersInFlight()
        val id = UUID.randomUUID()
        assertTrue(inFlight.tryStart(id))
        inFlight.finish(id)
        assertFalse(inFlight.isRunning(id))
        assertTrue(inFlight.tryStart(id))
    }

    @Test
    fun `transfers of different players do not block each other`() {
        val inFlight = TransfersInFlight()
        assertTrue(inFlight.tryStart(UUID.randomUUID()))
        assertTrue(inFlight.tryStart(UUID.randomUUID()))
    }
}
