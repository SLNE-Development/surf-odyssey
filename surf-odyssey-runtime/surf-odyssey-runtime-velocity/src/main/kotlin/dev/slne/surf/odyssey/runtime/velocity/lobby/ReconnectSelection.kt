package dev.slne.surf.odyssey.runtime.velocity.lobby

import dev.slne.surf.odyssey.core.common.reconnect.ReconnectEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * Returns whether [category] belongs to a hub world.
 *
 * @param category a server category
 * @return `true` if the category starts with [ReconnectEntry.HUB_CATEGORY_PREFIX]
 */
fun isHubCategory(category: String): Boolean = category.startsWith(ReconnectEntry.HUB_CATEGORY_PREFIX)

/**
 * Picks the initial server of a joining player who last left the hub server [lastServer].
 *
 * The last server wins while it is running. Otherwise the running server of the same category with the fewest
 * players is chosen, and if none of them runs, the running lobby with the fewest players. Ties are broken by the
 * server name in natural order.
 *
 * @param lastServer the server the player last left, or `null` if there is no hub server to return to
 * @param lastCategoryServers the known servers of the last server's category
 * @param lobbies the known lobby servers
 * @return the name of the chosen server, or `null` if neither a server of the category nor a lobby is running
 */
fun pickInitialServer(
    lastServer: String?,
    lastCategoryServers: List<LobbyCandidate>,
    lobbies: List<LobbyCandidate>,
): String? {
    if (lastServer != null) {
        lastCategoryServers.firstOrNull { it.name == lastServer && it.running }?.let { return it.name }
        pickEmptiestLobby(lastCategoryServers)?.let { return it }
    }
    return pickEmptiestLobby(lobbies)
}

/**
 * Chooses the initial server of a joining player from their reconnect entry and the servers surf-core knows.
 *
 * The entry counts only if it belongs to a hub category. If [lookUp] fails or does not answer within [timeout], the
 * player is treated as having no entry and [onLookupFailure] is told about a failure.
 *
 * @param lookUp reads the player's reconnect entry, or returns `null` if there is none
 * @param candidates returns the known servers of a category
 * @param timeout how long [lookUp] may take
 * @param onLookupFailure receives the error if [lookUp] throws
 * @return the name of the chosen server, as [pickInitialServer] decides, or `null` if nothing suitable runs
 */
suspend fun chooseInitialServer(
    lookUp: suspend () -> ReconnectEntry?,
    candidates: (String) -> List<LobbyCandidate>,
    timeout: Duration,
    onLookupFailure: (Exception) -> Unit,
): String? {
    val entry = try {
        withTimeoutOrNull(timeout) { lookUp() }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        onLookupFailure(exception)
        null
    }?.takeIf { isHubCategory(it.category) }
    val categoryServers = entry?.let { candidates(it.category) } ?: emptyList()
    return pickInitialServer(entry?.serverName, categoryServers, candidates(InitialLobbyListener.LOBBY_CATEGORY))
}
