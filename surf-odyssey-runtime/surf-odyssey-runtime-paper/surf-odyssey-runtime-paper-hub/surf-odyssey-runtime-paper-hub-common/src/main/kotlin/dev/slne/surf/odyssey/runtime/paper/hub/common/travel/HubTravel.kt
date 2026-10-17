package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.core.api.common.server.state.SurfServerState
import org.bukkit.entity.Player

/** The steps of a journey that touch the network and the traveller. */
interface TravelGateway {
    /** Leaves word that the traveller is about to arrive on [targetServer]. */
    suspend fun announceArrival(targetServer: String)

    /** Sends the traveller to [targetServer] and returns whether the transfer succeeded. */
    suspend fun transfer(targetServer: String): Boolean

    /** Takes back the word left by [announceArrival]. */
    suspend fun withdrawArrival()

    /** Shows the traveller the error [message]. */
    fun tell(message: String)
}

/** Sends players to the least crowded server of a world category. */
object HubTravel {
    /** Shown when no server of the target category is running. */
    const val UNREACHABLE = "Dieses Ziel ist gerade nicht erreichbar."

    /** Shown when the transfer to the chosen server fails. */
    const val FAILED = "Die Reise ist fehlgeschlagen. Versuche es gleich noch einmal."

    /**
     * Sends [player] to the running server of [category] with the fewest players and returns whether it worked.
     *
     * Before the transfer, the arrival is announced so that the target server places the player in front of
     * the portal leading back to this server's category.
     */
    suspend fun send(player: Player, category: String): Boolean {
        val candidates = SurfCoreApi.getServerByCategory(category)
            .map { ServerCandidate(it.name, it.state == SurfServerState.RUNNING, it.getPlayerCount()) }
        return travel(candidates, PlayerGateway(player))
    }

    /**
     * Chooses a target among [candidates] and sends the traveller there through [gateway].
     *
     * Without a running candidate the traveller is told so and nothing else happens. Otherwise the arrival is
     * announced, the traveller is transferred, and if the transfer fails the announcement is withdrawn and the
     * traveller is told so.
     *
     * @return whether the traveller was transferred
     */
    suspend fun travel(candidates: Collection<ServerCandidate>, gateway: TravelGateway): Boolean {
        val target = ServerChooser.choose(candidates)
        if (target == null) {
            gateway.tell(UNREACHABLE)
            return false
        }
        gateway.announceArrival(target.name)
        if (gateway.transfer(target.name)) return true
        gateway.withdrawArrival()
        gateway.tell(FAILED)
        return false
    }

    /** Carries out a journey of [player] through surf-core and the arrival cache. */
    private class PlayerGateway(private val player: Player) : TravelGateway {
        /** Stores an arrival ticket from this server's category to [targetServer]. */
        override suspend fun announceArrival(targetServer: String) =
            ArrivalCache.put(player.uniqueId, ArrivalTicket(SurfCoreApi.getCurrentServerCategory(), targetServer))

        /** Sends the player to [targetServer]; fails if surf-core does not know the player or the server. */
        override suspend fun transfer(targetServer: String): Boolean {
            val surfPlayer = SurfCoreApi.getPlayer(player.uniqueId) ?: return false
            val server = SurfCoreApi.getServerByName(targetServer) ?: return false
            return surfPlayer.sendAwaiting(server).isSuccessful()
        }

        /** Removes the player's arrival ticket. */
        override suspend fun withdrawArrival() = ArrivalCache.invalidate(player.uniqueId)

        /** Sends [message] to the player as an error. */
        override fun tell(message: String) = player.sendText { appendErrorPrefix(); error(message) }
    }
}
