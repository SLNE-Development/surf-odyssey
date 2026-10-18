package dev.slne.surf.odyssey.runtime.paper.common.trade

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TradeSessionSnapshotTest {
    private val first = TradeSide(UUID.randomUUID(), "first")
    private val second = TradeSide(UUID.randomUUID(), "second")
    private val session = TradeSession(first, second)

    @Test
    fun `has no snapshot while the trade is running`() {
        assertNull(session.takeSnapshot())
    }

    @Test
    fun `snapshots a cancelled trade once`() {
        session.cancel(second.uuid, TradeCancelReason.CLOSED)

        val snapshot = assertNotNull(session.takeSnapshot())

        assertEquals(session.id, snapshot.id)
        assertEquals(first.uuid, snapshot.first)
        assertEquals(second.uuid, snapshot.second)
        assertFalse(snapshot.completed)
        assertEquals(TradeCancelReason.CLOSED, snapshot.cancelReason)
        assertEquals(second.uuid, snapshot.cancelledBy)
        assertTrue(snapshot.endedAt >= snapshot.startedAt)
        assertNull(session.takeSnapshot())
    }

    @Test
    fun `a cancel without a player has no canceller`() {
        session.cancel(null, TradeCancelReason.SHUTDOWN)

        val snapshot = assertNotNull(session.takeSnapshot())

        assertEquals(TradeCancelReason.SHUTDOWN, snapshot.cancelReason)
        assertNull(snapshot.cancelledBy)
    }
}
