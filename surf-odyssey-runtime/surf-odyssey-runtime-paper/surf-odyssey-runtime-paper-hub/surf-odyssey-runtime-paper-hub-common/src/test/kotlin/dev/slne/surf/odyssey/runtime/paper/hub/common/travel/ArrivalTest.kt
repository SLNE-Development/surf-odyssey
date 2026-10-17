package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Cuboid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArrivalTest {
    private val shape = Cuboid.of(BlockPoint(0, 60, 0), BlockPoint(2, 63, 0))
    private val toNorth = Portal("to-north", shape, "hub-north", WorldPosition(1.5, 60.0, 2.5, 180f, 0f))
    private val toLobby = Portal("to-lobby", shape, "hub-lobby", WorldPosition(10.5, 64.0, 10.5, 90f, 10f))
    private val portals = listOf(toNorth, toLobby)

    @Test
    fun `a traveller lands in front of the portal leading back to where they came from`() = assertEquals(
        toLobby.arrival,
        Arrival.resolve(ArrivalTicket("hub-lobby", "hub-north-01"), "hub-north-01", portals),
    )

    @Test
    fun `no ticket means no arrival point`() = assertNull(Arrival.resolve(null, "hub-north-01", portals))

    @Test
    fun `a ticket for another server is ignored`() =
        assertNull(Arrival.resolve(ArrivalTicket("hub-lobby", "hub-north-02"), "hub-north-01", portals))

    @Test
    fun `a ticket from a category without a portal back is ignored`() =
        assertNull(Arrival.resolve(ArrivalTicket("hub-west", "hub-north-01"), "hub-north-01", portals))
}
