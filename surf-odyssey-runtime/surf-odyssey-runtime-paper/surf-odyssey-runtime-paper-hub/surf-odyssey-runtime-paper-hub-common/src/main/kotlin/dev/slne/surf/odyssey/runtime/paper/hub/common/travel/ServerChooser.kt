package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

/**
 * A server a player could be sent to.
 *
 * @property name the server's name in the network
 * @property running whether the server accepts players
 * @property players how many players are on the server
 */
data class ServerCandidate(val name: String, val running: Boolean, val players: Int)

/** Picks the server a traveller is sent to. */
object ServerChooser {
    /** Returns the running candidate with the fewest players, ties broken by name, or `null` if none runs. */
    fun choose(candidates: Collection<ServerCandidate>): ServerCandidate? =
        candidates.filter { it.running }.minWithOrNull(compareBy<ServerCandidate> { it.players }.thenBy { it.name })
}
