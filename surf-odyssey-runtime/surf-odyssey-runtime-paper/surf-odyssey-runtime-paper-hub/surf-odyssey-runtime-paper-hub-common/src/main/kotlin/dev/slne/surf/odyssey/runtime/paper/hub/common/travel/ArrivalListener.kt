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

/**
 * Chooses where joining players spawn.
 *
 * Players who came through a portal appear in front of the portal leading back to where they came from;
 * everyone else appears at the hub's spawn point, or at the world's spawn if the hub has none.
 */
object ArrivalListener : Listener {
    private val LOOKUP_TIMEOUT = 2.seconds

    /**
     * Sets the spawn location to the arrival point of the joining player's ticket, or to the spawn point.
     *
     * A ticket counts only if it is meant for this server and a portal leads back to its category. The lookup
     * blocks the async spawn thread for at most two seconds; on timeout or error the player is treated as
     * having no ticket.
     */
    @EventHandler
    fun onSpawnLocation(event: AsyncPlayerSpawnLocationEvent) {
        val hub = HubPlugin.instance
        val arrival = event.connection.profile.id?.let { lookUp(it, hub) }
        val position = Arrival.spawnPosition(arrival, hub.spawnPoint)
        event.spawnLocation = position?.toLocation(hub.hubWorld) ?: hub.hubWorld.spawnLocation
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
