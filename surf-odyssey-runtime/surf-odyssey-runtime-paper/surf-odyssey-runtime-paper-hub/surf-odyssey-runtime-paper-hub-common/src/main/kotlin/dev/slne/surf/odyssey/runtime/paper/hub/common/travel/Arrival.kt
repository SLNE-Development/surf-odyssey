package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import kotlinx.serialization.Serializable

/**
 * Word that a player travelling through a portal is about to arrive on another server.
 *
 * @property sourceCategory the server category the player left
 * @property targetServerName the name of the server the player was sent to
 */
@Serializable
data class ArrivalTicket(val sourceCategory: String, val targetServerName: String)

/** Decides where an arriving traveller appears. */
object Arrival {
    /**
     * Returns the arrival point of the portal in [portals] that leads back to the category the [ticket] came from.
     *
     * @param ticket the traveller's ticket, or `null` if none was found
     * @param serverName the name of this server
     * @param portals the portals of this server's world
     * @return the arrival point, or `null` if there is no ticket, the ticket is for another server, or no portal
     *         leads back to the ticket's category
     */
    fun resolve(ticket: ArrivalTicket?, serverName: String, portals: List<Portal>): WorldPosition? {
        if (ticket == null || ticket.targetServerName != serverName) return null
        return portals.firstOrNull { it.targetCategory == ticket.sourceCategory }?.arrival
    }
}
