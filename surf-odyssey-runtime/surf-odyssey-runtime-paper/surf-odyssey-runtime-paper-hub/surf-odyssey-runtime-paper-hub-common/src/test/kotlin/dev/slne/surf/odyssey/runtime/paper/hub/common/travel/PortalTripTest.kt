package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Checks when [sendOrCancelDeparture] withdraws a portal departure. */
class PortalTripTest {
    private var cancelled = 0

    @Test
    fun `a successful transfer keeps the departure`() = runBlocking {
        sendOrCancelDeparture({ true }) { cancelled++ }
        assertEquals(0, cancelled)
    }

    @Test
    fun `a failed transfer cancels the departure`() = runBlocking {
        sendOrCancelDeparture({ false }) { cancelled++ }
        assertEquals(1, cancelled)
    }

    @Test
    fun `a throwing transfer cancels the departure and passes the error on`() = runBlocking {
        assertFailsWith<IllegalStateException> { sendOrCancelDeparture({ error("boom") }) { cancelled++ } }
        assertEquals(1, cancelled)
    }

    @Test
    fun `a cancelled transfer cancels the departure and stays cancelled`() = runBlocking {
        assertFailsWith<CancellationException> { sendOrCancelDeparture({ throw CancellationException("stop") }) { cancelled++ } }
        assertEquals(1, cancelled)
    }
}
