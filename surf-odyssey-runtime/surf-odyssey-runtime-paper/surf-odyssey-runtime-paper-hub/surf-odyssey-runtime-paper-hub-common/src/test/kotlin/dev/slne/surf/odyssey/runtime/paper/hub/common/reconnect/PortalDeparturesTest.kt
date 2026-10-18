package dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect

import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests which position is stored for players who leave, with and without a portal transfer running. */
class PortalDeparturesTest {
    private val player = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val other = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val insidePortal = WorldPosition(1.5, 61.0, 0.5, 0f, 0f)
    private val inFrontOfPortal = WorldPosition(1.5, 60.0, 2.5, 180f, 0f)

    @Test
    fun `a player leaving without a portal transfer keeps their position`() =
        assertEquals(insidePortal, PortalDepartures().quitPosition(player, insidePortal))

    @Test
    fun `a player leaving through a portal is placed in front of that portal`() {
        val departures = PortalDepartures()
        departures.depart(player, inFrontOfPortal)

        assertEquals(inFrontOfPortal, departures.quitPosition(player, insidePortal))
    }

    @Test
    fun `a departure is used only once`() {
        val departures = PortalDepartures()
        departures.depart(player, inFrontOfPortal)
        departures.quitPosition(player, insidePortal)

        assertEquals(insidePortal, departures.quitPosition(player, insidePortal))
    }

    @Test
    fun `a cancelled departure no longer applies`() {
        val departures = PortalDepartures()
        departures.depart(player, inFrontOfPortal)
        departures.cancel(player)

        assertEquals(insidePortal, departures.quitPosition(player, insidePortal))
    }

    @Test
    fun `a departure applies only to its own player`() {
        val departures = PortalDepartures()
        departures.depart(other, inFrontOfPortal)

        assertEquals(insidePortal, departures.quitPosition(player, insidePortal))
    }
}
