package dev.slne.surf.odyssey.runtime.velocity.lobby

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests the lobby selection rule used for the initial server. */
class LobbySelectionTest {

    @Test
    fun `picks the running lobby with the fewest players`() {
        val lobbies = listOf(
            LobbyCandidate("lobby-1", running = true, playerCount = 12),
            LobbyCandidate("lobby-2", running = true, playerCount = 3),
            LobbyCandidate("lobby-3", running = true, playerCount = 7)
        )

        assertEquals("lobby-2", pickEmptiestLobby(lobbies))
    }

    @Test
    fun `breaks ties by server name`() {
        val lobbies = listOf(
            LobbyCandidate("lobby-b", running = true, playerCount = 4),
            LobbyCandidate("lobby-a", running = true, playerCount = 4)
        )

        assertEquals("lobby-a", pickEmptiestLobby(lobbies))
    }

    @Test
    fun `ignores lobbies that are not running`() {
        val lobbies = listOf(
            LobbyCandidate("lobby-1", running = false, playerCount = 0),
            LobbyCandidate("lobby-2", running = true, playerCount = 9)
        )

        assertEquals("lobby-2", pickEmptiestLobby(lobbies))
    }

    @Test
    fun `returns null when no lobby is running`() {
        val lobbies = listOf(LobbyCandidate("lobby-1", running = false, playerCount = 0))

        assertNull(pickEmptiestLobby(lobbies))
        assertNull(pickEmptiestLobby(emptyList()))
    }
}
