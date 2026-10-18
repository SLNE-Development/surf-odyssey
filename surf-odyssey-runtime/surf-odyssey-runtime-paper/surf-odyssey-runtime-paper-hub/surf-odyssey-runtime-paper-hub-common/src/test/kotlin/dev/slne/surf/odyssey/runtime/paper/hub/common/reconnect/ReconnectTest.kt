package dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect

import dev.slne.surf.odyssey.core.common.reconnect.ReconnectEntry
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests which logout position a reconnecting player gets back. */
class ReconnectTest {
    private val entry = ReconnectEntry("odyssey-hub-north", "hub-north-01", 12.5, 70.0, -3.5, 90f, 15f, "abc")

    @Test
    fun `same category and same map give the logout position`() = assertEquals(
        WorldPosition(12.5, 70.0, -3.5, 90f, 15f),
        Reconnect.logoutPosition(entry, "odyssey-hub-north", "abc"),
    )

    @Test
    fun `the logout position applies on another server of the same category`() = assertEquals(
        WorldPosition(12.5, 70.0, -3.5, 90f, 15f),
        Reconnect.logoutPosition(entry.copy(serverName = "hub-north-02"), "odyssey-hub-north", "abc"),
    )

    @Test
    fun `a changed map gives no logout position`() =
        assertNull(Reconnect.logoutPosition(entry, "odyssey-hub-north", "def"))

    @Test
    fun `an entry of another category gives no logout position`() =
        assertNull(Reconnect.logoutPosition(entry, "odyssey-hub-south", "abc"))

    @Test
    fun `a server without map hash gives no logout position`() =
        assertNull(Reconnect.logoutPosition(entry, "odyssey-hub-north", null))

    @Test
    fun `an entry without map hash gives no logout position`() =
        assertNull(Reconnect.logoutPosition(entry.copy(worldHash = null), "odyssey-hub-north", "abc"))

    @Test
    fun `entry and server both without map hash give no logout position`() =
        assertNull(Reconnect.logoutPosition(entry.copy(worldHash = null), "odyssey-hub-north", null))

    @Test
    fun `no entry gives no logout position`() =
        assertNull(Reconnect.logoutPosition(null, "odyssey-hub-north", "abc"))

    @Test
    fun `an entry is built from the position on this server`() = assertEquals(
        entry,
        Reconnect.entry("odyssey-hub-north", "hub-north-01", WorldPosition(12.5, 70.0, -3.5, 90f, 15f), "abc"),
    )
}
