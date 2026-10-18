package dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect

import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers the players whose portal transfer is running, with the arrival point in front of the portal they used.
 *
 * A player leaving during such a transfer is recorded in front of the portal instead of inside it.
 */
class PortalDepartures {
    private val departures = ConcurrentHashMap<UUID, WorldPosition>()

    /** Notes that [player] is leaving through the portal whose arrival point is [inFrontOfPortal]. */
    fun depart(player: UUID, inFrontOfPortal: WorldPosition) {
        departures[player] = inFrontOfPortal
    }

    /** Forgets the departure of [player], whose transfer did not happen. */
    fun cancel(player: UUID) {
        departures.remove(player)
    }

    /**
     * Returns the position to store for [player], who is leaving at [current], and forgets their departure.
     *
     * @return the arrival point in front of the portal if [player] is leaving through one, otherwise [current]
     */
    fun quitPosition(player: UUID, current: WorldPosition): WorldPosition = departures.remove(player) ?: current
}
