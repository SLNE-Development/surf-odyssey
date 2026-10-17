package dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.odyssey.core.common.reconnect.ReconnectEntry
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.bukkit.entity.Player
import java.util.UUID
import java.util.logging.Level
import kotlin.time.Duration.Companion.seconds

/**
 * Remembers where players leave this hub server and gives the position back when they return.
 *
 * At start, the hash of this server's map is computed. When a player leaves, an entry with this server, the logout
 * position and the hash is stored. A returning player gets the logout position back only on a server of the same
 * category whose map has the same hash.
 */
object ReconnectService {
    /**
     * Environment variable naming the folder the world of this server is copied from.
     *
     * If it names a readable directory, the map hash is computed from that folder, which the server never writes to.
     * Otherwise this server has no map hash and returning players never get their logout position back here.
     */
    const val WORLD_SOURCE_VARIABLE = "WORLD"

    private val LOOKUP_TIMEOUT = 2.seconds
    private val SHUTDOWN_TIMEOUT = 5.seconds

    /** Hash of the map this server runs, or `null` if it is unknown. Set by [start]. */
    var worldHash: String? = null
        private set

    /**
     * Computes the map hash from the folder named by [WORLD_SOURCE_VARIABLE], connects to Redis and starts recording
     * players who leave.
     *
     * Without a readable source folder, a warning is logged and the server runs without map hash.
     */
    fun start(hub: HubPlugin) {
        val configured = System.getenv(WORLD_SOURCE_VARIABLE)
        val source = WorldHash.source(configured)
        worldHash = source?.let(WorldHash::compute)
        if (source == null) {
            hub.logger.warning(
                "$WORLD_SOURCE_VARIABLE does not name a readable world folder (\"$configured\"); " +
                    "returning players spawn at the spawn point instead of their logout position"
            )
        } else {
            hub.logger.info("Map hash $worldHash computed from $source")
        }
        ReconnectCache.connect(hub.name)
        ReconnectListener.register()
    }

    /**
     * Stores an entry for every player still online, then disconnects from Redis.
     *
     * Storing gives up after five seconds. Errors are logged and never thrown.
     */
    suspend fun stop(hub: HubPlugin) {
        try {
            val entries = hub.server.onlinePlayers.mapNotNull { player -> entryOf(player, hub)?.let { player.uniqueId to it } }
            withTimeoutOrNull(SHUTDOWN_TIMEOUT) { entries.forEach { (player, entry) -> ReconnectCache.put(player, entry) } }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            hub.logger.log(Level.WARNING, "Could not store the logout positions on shutdown", exception)
        } finally {
            ReconnectCache.disconnect()
        }
    }

    /** Stores the entry of [player], who is leaving, in the background; errors are logged. */
    fun record(player: Player) {
        val hub = HubPlugin.instance
        val entry = entryOf(player, hub) ?: return
        val id = player.uniqueId
        hub.launch {
            try {
                ReconnectCache.put(id, entry)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                hub.logger.log(Level.WARNING, "Could not store the logout position of $id", exception)
            }
        }
    }

    /**
     * Returns the logout position of [player] if their entry belongs to this server's category and map.
     *
     * The lookup blocks for at most two seconds; on timeout or error there is no logout position.
     */
    fun logoutPosition(player: UUID, hub: HubPlugin): WorldPosition? = try {
        runBlocking {
            withTimeoutOrNull(LOOKUP_TIMEOUT) {
                Reconnect.logoutPosition(ReconnectCache.get(player), SurfCoreApi.getCurrentServerCategory(), worldHash)
            }
        }
    } catch (exception: Exception) {
        hub.logger.log(Level.WARNING, "Could not look up the logout position of $player", exception)
        null
    }

    /** Returns the entry for [player] at their current position, or `null` if they are not in the hub world. */
    private fun entryOf(player: Player, hub: HubPlugin): ReconnectEntry? {
        val location = player.location
        if (location.world != hub.hubWorld) return null
        return Reconnect.entry(
            SurfCoreApi.getCurrentServerCategory(),
            SurfCoreApi.getCurrentServerName(),
            WorldPosition(location.x, location.y, location.z, location.yaw, location.pitch),
            worldHash,
        )
    }
}
