package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.core.api.common.server.state.SurfServerState
import org.bukkit.entity.Player

/** The steps of a journey that touch the network and the traveller. */
interface TravelGateway {
    /** Sends the traveller to [targetServer] and returns whether the transfer succeeded. */
    suspend fun transfer(targetServer: String): Boolean

    /** Shows the traveller the error [message]. */
    fun tell(message: String)
}

/** Sends players to the least crowded server of a world category. */
object HubTravel {
    /** Shown when no server of the target category is running. */
    const val UNREACHABLE = "Dieses Ziel ist gerade nicht erreichbar."

    /** Shown when the transfer to the chosen server fails. */
    const val FAILED = "Die Reise ist fehlgeschlagen. Versuche es gleich noch einmal."

    /** Sends [player] to the running server of [category] with the fewest players and returns whether it worked. */
    suspend fun send(player: Player, category: String): Boolean {
        val candidates = SurfCoreApi.getServerByCategory(category)
            .map { ServerCandidate(it.name, it.state == SurfServerState.RUNNING, it.getPlayerCount()) }
        return travel(candidates, PlayerGateway(player))
    }

    /**
     * Chooses a target among [candidates] and sends the traveller there through [gateway].
     *
     * Without a running candidate the traveller is told so and nothing else happens. If the transfer fails,
     * the traveller is told so as well.
     *
     * @return whether the traveller was transferred
     */
    suspend fun travel(candidates: Collection<ServerCandidate>, gateway: TravelGateway): Boolean {
        val target = ServerChooser.choose(candidates)
        if (target == null) {
            gateway.tell(UNREACHABLE)
            return false
        }
        if (gateway.transfer(target.name)) return true
        gateway.tell(FAILED)
        return false
    }

    /** Carries out a journey of [player] through surf-core. */
    private class PlayerGateway(private val player: Player) : TravelGateway {
        /** Sends the player to [targetServer]; fails if surf-core does not know the player or the server. */
        override suspend fun transfer(targetServer: String): Boolean {
            val surfPlayer = SurfCoreApi.getPlayer(player.uniqueId) ?: return false
            val server = SurfCoreApi.getServerByName(targetServer) ?: return false
            return surfPlayer.sendAwaiting(server).isSuccessful()
        }

        /** Sends [message] to the player as an error. */
        override fun tell(message: String) = player.sendText { appendErrorPrefix(); error(message) }
    }
}
