package dev.slne.surf.odyssey.runtime.velocity.lobby

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests the initial server choice for players who left from a hub server. */
class ReconnectSelectionTest {
    private val north = listOf(
        LobbyCandidate("hub-north-01", running = true, playerCount = 9),
        LobbyCandidate("hub-north-02", running = true, playerCount = 2),
    )
    private val lobbies = listOf(
        LobbyCandidate("hub-lobby-01", running = true, playerCount = 5),
        LobbyCandidate("hub-lobby-02", running = true, playerCount = 1),
    )

    @Test
    fun `the last server is chosen while it runs, even if it is fuller`() =
        assertEquals("hub-north-01", pickInitialServer("hub-north-01", north, lobbies))

    @Test
    fun `if the last server is down the emptiest running server of its category is chosen`() {
        val lastDown = listOf(north[0].copy(running = false), north[1])

        assertEquals("hub-north-02", pickInitialServer("hub-north-01", lastDown, lobbies))
    }

    @Test
    fun `if the last server is gone the emptiest running server of its category is chosen`() =
        assertEquals("hub-north-02", pickInitialServer("hub-north-03", north, lobbies))

    @Test
    fun `if no server of the category runs the emptiest lobby is chosen`() {
        val allDown = north.map { it.copy(running = false) }

        assertEquals("hub-lobby-02", pickInitialServer("hub-north-01", allDown, lobbies))
        assertEquals("hub-lobby-02", pickInitialServer("hub-north-01", emptyList(), lobbies))
    }

    @Test
    fun `without a last server the emptiest lobby is chosen`() =
        assertEquals("hub-lobby-02", pickInitialServer(null, emptyList(), lobbies))

    @Test
    fun `without any running server nothing is chosen`() =
        assertNull(pickInitialServer("hub-north-01", emptyList(), emptyList()))

    @Test
    fun `hub categories are recognised by their prefix`() {
        assertTrue(isHubCategory("odyssey-hub-lobby"))
        assertTrue(isHubCategory("odyssey-hub-north"))
        assertFalse(isHubCategory("home"))
        assertFalse(isHubCategory("odyssey-pack"))
    }
}
