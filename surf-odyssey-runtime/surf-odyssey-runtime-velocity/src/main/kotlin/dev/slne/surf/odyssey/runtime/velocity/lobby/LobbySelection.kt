package dev.slne.surf.odyssey.runtime.velocity.lobby

/**
 * A lobby server as seen by the selection rule.
 *
 * @property name the server name registered at the proxy
 * @property running whether the server is in the running state
 * @property playerCount the number of players currently on the server
 */
data class LobbyCandidate(val name: String, val running: Boolean, val playerCount: Int)

/**
 * Picks the lobby a joining player is sent to.
 *
 * Only running lobbies are considered. The lobby with the fewest players wins; ties are
 * broken by the server name in natural order.
 *
 * @param candidates the known lobby servers
 * @return the name of the chosen lobby, or `null` if no lobby is running
 */
fun pickEmptiestLobby(candidates: List<LobbyCandidate>): String? =
    candidates
        .filter { it.running }
        .minWithOrNull(compareBy({ it.playerCount }, { it.name }))
        ?.name
