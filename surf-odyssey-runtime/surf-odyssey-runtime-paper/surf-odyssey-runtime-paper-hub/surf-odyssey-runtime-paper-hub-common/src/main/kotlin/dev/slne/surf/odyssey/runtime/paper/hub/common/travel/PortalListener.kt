package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.gate.GateAccess
import dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect.ReconnectService
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.core.util.Key
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Sends a player who steps onto a portal block inside a portal area to the portal's target.
 *
 * A player can use portals at most once every three seconds and never while a transfer is still running. A portal
 * whose gate locks the player out sends nobody and tells them so in the action bar. While the
 * transfer runs, the player is noted as leaving through the portal, so that leaving records them in front of it; the
 * note is dropped again if the transfer fails or throws.
 */
object PortalListener : Listener {
    /** CraftEngine block that marks portal surfaces. */
    val PORTAL_BLOCK: Key = Key.of("odyssey:portal")

    private val cooldown = PortalCooldown(3_000)
    private val inFlight = TransfersInFlight()

    /** Checks the blocks at the player's feet and head against the portals of this world. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        if (!event.hasChangedBlock()) return
        val hub = HubPlugin.instance
        val to = event.to
        if (to.world != hub.hubWorld) return
        val feet = to.block
        val head = feet.getRelative(0, 1, 0)
        val portal = hub.portalList.firstOrNull { portal ->
            listOf(feet, head).any { portal.shape.contains(it.x, it.y, it.z) && isPortalBlock(it) }
        } ?: return
        val player = event.player
        val id = player.uniqueId
        if (inFlight.isRunning(id)) return
        if (!cooldown.tryUse(id, System.currentTimeMillis())) return
        if (GateAccess.isLockedOut(player, portal.gate)) {
            GateAccess.tellPortalLocked(player)
            return
        }
        if (!inFlight.tryStart(id)) return
        ReconnectService.departThroughPortal(id, portal.arrival)
        hub.launch {
            try {
                sendOrCancelDeparture({ HubTravel.send(player, portal.targetCategory) }) { ReconnectService.cancelDeparture(id) }
            } finally {
                inFlight.finish(id)
            }
        }
    }

    /** Forgets the player's cooldown and running transfer. */
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        cooldown.clear(event.player.uniqueId)
        inFlight.finish(event.player.uniqueId)
    }

    /** Returns whether [block] is a CraftEngine [PORTAL_BLOCK]. */
    private fun isPortalBlock(block: Block): Boolean {
        val state = CraftEngineBlocks.getCustomBlockState(block) ?: return false
        return !state.isEmpty && state.owner().value().id() == PORTAL_BLOCK
    }
}
