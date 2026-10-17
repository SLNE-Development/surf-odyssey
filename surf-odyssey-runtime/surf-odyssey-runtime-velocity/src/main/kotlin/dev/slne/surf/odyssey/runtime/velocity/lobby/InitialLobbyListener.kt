package dev.slne.surf.odyssey.runtime.velocity.lobby

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.core.api.common.server.state.SurfServerState
import dev.slne.surf.odyssey.runtime.velocity.server

/** Sends joining players to the running lobby with the fewest players instead of Velocity's try order. */
object InitialLobbyListener {
    /** Category of the lobby servers. */
    const val LOBBY_CATEGORY = "odyssey-hub-lobby"

    /** Picks the initial server; keeps Velocity's choice when no lobby is known to surf-core. */
    @Subscribe
    fun onChoose(event: PlayerChooseInitialServerEvent) {
        val candidates = SurfCoreApi.getServerByCategory(LOBBY_CATEGORY).map {
            LobbyCandidate(it.name, it.state == SurfServerState.RUNNING, it.getPlayerCount())
        }
        val name = pickEmptiestLobby(candidates) ?: return
        server.getServer(name).ifPresent { event.setInitialServer(it) }
    }
}
