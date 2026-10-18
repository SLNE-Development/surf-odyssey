package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPermissions
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Lets editors draw with the [RegionWand]: right-clicking a block adds it as a point of the
 * newest part, left-clicking a block removes the newest point of that part.
 */
object RegionWandListener : Listener {
    /**
     * Adds or removes a point when a player with the editor permission clicks a block with the
     * wand in the main hand, and cancels the interaction. Other clicks are left alone.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val block = event.clickedBlock ?: return
        val action = event.action
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return
        if (!RegionWand.isWand(event.item)) return
        val player = event.player
        if (!player.hasPermission(HubPermissions.REGION_EDITOR)) return
        event.isCancelled = true
        withSession(player) { session ->
            if (action == Action.RIGHT_CLICK_BLOCK) {
                if (session.addPoint(BlockPoint(block.x, block.y, block.z))) ok(player, "Punkt ${block.x} ${block.y} ${block.z} gesetzt.")
                else fail(player, "Starte zuerst einen Teil. Ein Quader hat höchstens zwei Ecken.")
            } else {
                if (session.removeLastPoint()) ok(player, "Letzten Punkt entfernt.")
                else fail(player, "Der aktuelle Teil hat keine Punkte.")
            }
        }
    }
}
