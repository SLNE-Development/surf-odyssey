package dev.slne.surf.odyssey.runtime.velocity.lobby

import com.github.shynixn.mccoroutine.velocity.launch
import com.github.shynixn.mccoroutine.velocity.scope
import com.velocitypowered.api.event.EventTask
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.core.api.common.server.state.SurfServerState
import dev.slne.surf.odyssey.runtime.velocity.container
import dev.slne.surf.odyssey.runtime.velocity.server
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
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
     * to the emptiest running server of that category, otherwise to the emptiest running lobby. The choice runs as a
     * coroutine without blocking the event thread, and Velocity waits for it before continuing the login. The entry
     * lookup may take at most two seconds; on timeout or error the player is treated as having no entry. If the
     * choice fails, Velocity's choice is kept.
     */
    @Subscribe
    fun onChoose(event: PlayerChooseInitialServerEvent): EventTask = EventTask.withContinuation { continuation ->
        if (!container.scope.isActive) {
            continuation.resume()
            return@withContinuation
        }
        container.launch {
            try {
                val player = event.player.uniqueId
                val name = chooseInitialServer({ ReconnectLookup.get(player) }, ::candidates, LOOKUP_TIMEOUT) {
                    log.atWarning().withCause(it).log("Could not look up the reconnect entry of %s", player)
                }
                name?.let { server.getServer(it).ifPresent(event::setInitialServer) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                log.atWarning().withCause(exception)
                    .log("Could not choose the initial server of %s", event.player.username)
            } finally {
                continuation.resume()
            }
        }
    }

    /** Returns the servers of [category] as surf-core knows them. */
    private fun candidates(category: String): List<LobbyCandidate> = SurfCoreApi.getServerByCategory(category).map {
        LobbyCandidate(it.name, it.state == SurfServerState.RUNNING, it.getPlayerCount())
    }
}
