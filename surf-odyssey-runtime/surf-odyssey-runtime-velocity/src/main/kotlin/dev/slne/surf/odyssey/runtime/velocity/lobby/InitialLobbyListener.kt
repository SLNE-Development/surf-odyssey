package dev.slne.surf.odyssey.runtime.velocity.lobby

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.core.api.common.server.state.SurfServerState
import dev.slne.surf.odyssey.core.common.reconnect.ReconnectEntry
import dev.slne.surf.odyssey.runtime.velocity.server
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Sends joining players back to the hub server they last left, or to the running lobby with the fewest players,
 * instead of Velocity's try order.
 */
object InitialLobbyListener {
    /** Category of the lobby servers. */
    const val LOBBY_CATEGORY = "odyssey-hub-lobby"

    private val LOOKUP_TIMEOUT = 2.seconds
    private val log = logger()

    /**
     * Picks the initial server; keeps Velocity's choice when surf-core knows no running server to pick.
     *
     * A player with a reconnect entry of a hub category goes back to the server named in it while it runs, otherwise
     * to the emptiest running server of that category, otherwise to the emptiest running lobby. The entry lookup
     * blocks for at most two seconds; on timeout or error the player is treated as having no entry.
     */
    @Subscribe
    fun onChoose(event: PlayerChooseInitialServerEvent) {
        val entry = lookUp(event.player.uniqueId)?.takeIf { isHubCategory(it.category) }
        val categoryServers = entry?.let { candidates(it.category) } ?: emptyList()
        val name = pickInitialServer(entry?.serverName, categoryServers, candidates(LOBBY_CATEGORY)) ?: return
        server.getServer(name).ifPresent { event.setInitialServer(it) }
    }

    /** Returns the servers of [category] as surf-core knows them. */
    private fun candidates(category: String): List<LobbyCandidate> = SurfCoreApi.getServerByCategory(category).map {
        LobbyCandidate(it.name, it.state == SurfServerState.RUNNING, it.getPlayerCount())
    }

    /** Returns the reconnect entry of [player], or `null` if there is none or it cannot be read in time. */
    private fun lookUp(player: UUID): ReconnectEntry? = try {
        runBlocking { withTimeoutOrNull(LOOKUP_TIMEOUT) { ReconnectLookup.get(player) } }
    } catch (exception: Exception) {
        log.atWarning().withCause(exception).log("Could not look up the reconnect entry of %s", player)
        null
    }
}
