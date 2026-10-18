package dev.slne.surf.odyssey.runtime.velocity.lobby

import com.github.shynixn.mccoroutine.velocity.launch
import com.github.shynixn.mccoroutine.velocity.scope
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.odyssey.runtime.velocity.container
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive

/**
 * Removes the reconnect entry of players who leave the network from a server that does not belong to a hub world,
 * e.g. their home, so that their next join leads to a lobby instead of the hub server they left before.
 */
object ReconnectCleanupListener {
    private val log = logger()

    /**
     * Removes the entry of the leaving player in the background if [forgetsReconnectEntry] decides so for the category
     * of the server they were on. Nothing happens once the plugin is shutting down. Errors are logged.
     */
    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        val player = event.player
        val serverName = player.currentServer.map { it.serverInfo.name }.orElse(null) ?: return
        val category = SurfCoreApi.getServerByName(serverName)?.category
        if (!forgetsReconnectEntry(category) || !container.scope.isActive) return
        val id = player.uniqueId
        container.launch {
            try {
                ReconnectLookup.delete(id)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                log.atWarning().withCause(exception).log("Could not remove the reconnect entry of %s", id)
            }
        }
    }
}
