package dev.slne.surf.odyssey.runtime.paper.common.trade

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TradeRequestBookTest {
    private val book = TradeRequestBook(timeoutMillis = 60_000L, cooldownMillis = 5_000L, maxOutgoing = 5)
    private val sender = UUID.randomUUID()

    private fun send(to: UUID, now: Long, from: UUID = sender) = book.add(from, "sender", to, "receiver", now)

    @Test
    fun `sends a request`() {
        assertEquals(TradeRequestResult.SENT, send(UUID.randomUUID(), now = 0))
    }

    @Test
    fun `refuses a second request during the cooldown`() {
        send(UUID.randomUUID(), now = 0)

        assertEquals(TradeRequestResult.COOLDOWN, send(UUID.randomUUID(), now = 4_999))
        assertEquals(1L, book.cooldownRemaining(sender, now = 4_999))
    }

    @Test
    fun `allows the next request once the cooldown passed`() {
        send(UUID.randomUUID(), now = 0)

        assertEquals(TradeRequestResult.SENT, send(UUID.randomUUID(), now = 5_000))
        assertEquals(0L, book.cooldownRemaining(sender, now = 10_000))
    }

    @Test
    fun `the cooldown only applies to the sender`() {
        send(UUID.randomUUID(), now = 0)

        assertEquals(TradeRequestResult.SENT, send(UUID.randomUUID(), now = 1, from = UUID.randomUUID()))
    }

    @Test
    fun `a refused request does not start the cooldown`() {
        val receiver = UUID.randomUUID()
        send(receiver, now = 0)

        assertEquals(TradeRequestResult.ALREADY_PENDING, send(receiver, now = 5_000))
        assertEquals(TradeRequestResult.SENT, send(UUID.randomUUID(), now = 5_001))
    }

    @Test
    fun `refuses a second request to the same player`() {
        val receiver = UUID.randomUUID()
        send(receiver, now = 0)

        assertEquals(TradeRequestResult.ALREADY_PENDING, send(receiver, now = 5_000))
    }

    @Test
    fun `refuses the sixth open request`() {
        repeat(5) { assertEquals(TradeRequestResult.SENT, send(UUID.randomUUID(), now = it * 5_000L)) }

        assertEquals(TradeRequestResult.LIMIT_REACHED, send(UUID.randomUUID(), now = 25_000))
    }

    @Test
    fun `expired requests do not count towards the limit`() {
        repeat(5) { send(UUID.randomUUID(), now = it * 5_000L) }

        assertEquals(TradeRequestResult.SENT, send(UUID.randomUUID(), now = 80_000))
    }

    @Test
    fun `a withdrawn request frees a slot`() {
        val first = UUID.randomUUID()
        send(first, now = 0)
        repeat(4) { send(UUID.randomUUID(), now = (it + 1) * 5_000L) }

        assertEquals(TradeRequestResult.LIMIT_REACHED, send(UUID.randomUUID(), now = 25_000))

        assertNotNull(book.take(sender, first))

        assertEquals(TradeRequestResult.SENT, send(UUID.randomUUID(), now = 25_000))
    }

    @Test
    fun `an expired request to the same player can be replaced`() {
        val receiver = UUID.randomUUID()
        send(receiver, now = 0)

        assertEquals(TradeRequestResult.SENT, send(receiver, now = 60_000))
    }

    @Test
    fun `takes a request once`() {
        val receiver = UUID.randomUUID()
        send(receiver, now = 0)

        assertNotNull(book.take(sender, receiver))
        assertNull(book.take(sender, receiver))
    }

    @Test
    fun `takes only the requests of the sender`() {
        val other = UUID.randomUUID()
        send(UUID.randomUUID(), now = 0)
        send(UUID.randomUUID(), now = 5_000)
        send(UUID.randomUUID(), now = 0, from = other)

        assertEquals(2, book.takeFrom(sender).size)
        assertEquals(1, book.involving(other, now = 5_000).size)
    }

    @Test
    fun `takes requests sent and received by a player`() {
        val player = UUID.randomUUID()
        send(player, now = 0)
        send(UUID.randomUUID(), now = 0, from = player)
        send(UUID.randomUUID(), now = 5_000)

        assertEquals(2, book.takeInvolving(player).size)
        assertEquals(1, book.takeAll().size)
    }

    @Test
    fun `takes expired requests only`() {
        send(UUID.randomUUID(), now = 0)
        send(UUID.randomUUID(), now = 50_000)

        val expired = book.takeExpired(now = 60_000)

        assertEquals(1, expired.size)
        assertTrue(expired.single().isExpired(60_000))
        assertEquals(1, book.takeAll().size)
    }

    @Test
    fun `lists received requests before sent ones and hides expired ones`() {
        val player = UUID.randomUUID()
        val partner = UUID.randomUUID()
        send(partner, now = 0, from = player)
        send(player, now = 1_000)
        send(UUID.randomUUID(), now = 2_000, from = UUID.randomUUID())

        val listed = book.involving(player, now = 3_000)

        assertEquals(listOf(player, partner), listed.map { it.receiver })
        assertEquals(emptyList(), book.involving(player, now = 120_000))
    }

    @Test
    fun `forgetting the cooldown lets the player send again`() {
        send(UUID.randomUUID(), now = 0)
        book.forgetCooldown(sender)

        assertEquals(TradeRequestResult.SENT, send(UUID.randomUUID(), now = 1))
    }
}
