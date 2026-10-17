package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.UUID
import java.util.logging.Level
import kotlin.time.Duration.Companion.seconds

/** Places players who came through a portal in front of the portal leading back to where they came from. */
object ArrivalListener : Listener {
    private val LOOKUP_TIMEOUT = 2.seconds

    /**
     * Looks up the joining player's arrival ticket and, if it is meant for this server and a portal leads back
     * to its category, spawns the player at that portal's arrival point.
     *
     * The lookup blocks the async spawn thread for at most two seconds; on timeout or error the spawn location
     * is left unchanged.
     */
    @EventHandler
    fun onSpawnLocation(event: AsyncPlayerSpawnLocationEvent) {
        val player = event.connection.profile.id ?: return
        val hub = HubPlugin.instance
        val arrival = lookUp(player, hub) ?: return
        event.spawnLocation = arrival.toLocation(hub.hubWorld)
    }

    /** Returns the arrival point for [player] and consumes the ticket, or `null` if there is none for this server. */
    private fun lookUp(player: UUID, hub: HubPlugin): WorldPosition? = try {
        runBlocking {
            withTimeoutOrNull(LOOKUP_TIMEOUT) {
                val serverName = SurfCoreApi.getCurrentServerName()
                val ticket = ArrivalCache.get(player)?.takeIf { it.targetServerName == serverName }
                    ?: return@withTimeoutOrNull null
                ArrivalCache.invalidate(player)
                Arrival.resolve(ticket, serverName, hub.portalList)
            }
        }
    } catch (exception: Exception) {
        hub.logger.log(Level.WARNING, "Could not look up the arrival of $player", exception)
        null
    }
}
