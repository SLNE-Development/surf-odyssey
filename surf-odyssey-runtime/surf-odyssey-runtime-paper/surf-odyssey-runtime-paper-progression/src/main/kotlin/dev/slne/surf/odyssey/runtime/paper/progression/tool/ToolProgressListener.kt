package dev.slne.surf.odyssey.runtime.paper.progression.tool

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

/**
 * Keeps the owner chain of tool items current: a player who joins with tools or picks one up becomes the newest
 * holder of each tool whose newest holder is someone else. A right-click into the air while sneaking with a tool in
 * the main hand opens its accessory view instead of using the tool.
 *
 * Usage itself is added by the region features through [ToolUsage.record].
 *
 * @param onTakenOver called with the player and the updated state of every tool the player took over
 */
class ToolProgressListener(private val onTakenOver: (Player, ToolState) -> Unit = { _, _ -> }) : Listener {
    /** Takes over every tool in the inventory of the joining player. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val inventory = player.inventory
        inventory.contents.forEachIndexed { slot, item ->
            val updated = item?.let { takeOver(player, it) } ?: return@forEachIndexed
            inventory.setItem(slot, updated)
        }
    }

    /** Takes over a tool a player picks up before it enters the inventory. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val updated = takeOver(player, event.item.itemStack) ?: return
        event.item.itemStack = updated
    }

    /** Opens the accessory view when a sneaking player right-clicks into the air with a tool in the main hand. */
    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR || event.hand != EquipmentSlot.HAND) return
        val player = event.player
        if (!player.isSneaking) return
        val state = ToolItems.read(player.inventory.itemInMainHand) ?: return
        if (ToolItems.definitionOf(state) == null) return
        event.setUseItemInHand(Event.Result.DENY)
        event.setUseInteractedBlock(Event.Result.DENY)
        openAccessoryView(player)
    }

    /**
     * Returns a copy of [item] with [player] as the newest holder and its lore rendered again, or `null` if [item] is
     * not a tool of the installed content or [player] already is its newest holder.
     */
    private fun takeOver(player: Player, item: ItemStack): ItemStack? {
        val state = ToolItems.read(item) ?: return null
        val tool = ToolItems.definitionOf(state) ?: return null
        if (state.chronicle.lastOwner == player.uniqueId) return null
        val updated = state.copy(chronicle = state.chronicle.withOwner(player.uniqueId, player.name))
        val copy = item.clone()
        ToolItems.write(copy, updated)
        ToolItems.refresh(copy, tool, updated)
        onTakenOver(player, updated)
        return copy
    }
}
