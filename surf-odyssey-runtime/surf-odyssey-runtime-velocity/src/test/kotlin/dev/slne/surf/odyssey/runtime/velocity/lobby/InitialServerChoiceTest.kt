package dev.slne.surf.odyssey.runtime.velocity.lobby

import dev.slne.surf.odyssey.core.common.reconnect.ReconnectEntry
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

/** Tests how the initial server is chosen from a reconnect lookup that may fail, hang or find nothing. */
class InitialServerChoiceTest {
    private val servers = mapOf(
        "odyssey-hub-north" to listOf(LobbyCandidate("hub-north-01", running = true, playerCount = 9)),
        "home" to listOf(LobbyCandidate("home", running = true, playerCount = 0)),
        InitialLobbyListener.LOBBY_CATEGORY to listOf(LobbyCandidate("hub-lobby-01", running = true, playerCount = 3)),
    )
    private val north = ReconnectEntry("odyssey-hub-north", "hub-north-01", 0.0, 64.0, 0.0, 0f, 0f, "abc")

    /** Runs the choice with the given [lookup] and a short timeout. */
    private fun choose(lookup: suspend () -> ReconnectEntry?): String? = runBlocking {
        chooseInitialServer(lookup, { servers[it].orEmpty() }, 50.milliseconds) {}
    }

    @Test
    fun `a hub entry leads back to its server`() = assertEquals("hub-north-01", choose { north })

    @Test
    fun `without an entry the emptiest lobby is chosen`() = assertEquals("hub-lobby-01", choose { null })

    @Test
    fun `an entry of a category outside the hubs is ignored`() =
        assertEquals("hub-lobby-01", choose { north.copy(category = "home", serverName = "home") })

    @Test
    fun `a failing lookup falls back to the emptiest lobby`() =
        assertEquals("hub-lobby-01", choose { error("Redis is down") })

    @Test
    fun `a lookup that does not answer in time falls back to the emptiest lobby`() =
        assertEquals("hub-lobby-01", choose { awaitCancellation() })
}
