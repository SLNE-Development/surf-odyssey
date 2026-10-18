package dev.slne.surf.odyssey.runtime.paper.progression.menu

import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.PlayerInventory
import org.bukkit.plugin.Plugin

/**
 * Keeps the [OdysseyBook] in the last hotbar slot of every player and opens the Odyssey menu with it.
 *
 * The book is put back on join, shortly after the join, on respawn and on a world change. It cannot be dropped,
 * moved, put into containers, item frames, armor stands or the off hand. Shift and right click opens the
 * [OdysseyMenuDialog]; a plain right click tells the player that research follows soon.
 *
 * @param plugin the plugin whose scheduler runs the delayed checks
 */
class OdysseyBookListener(private val plugin: Plugin) : Listener {
    /** Gives the joining player the book, and once more after [JOIN_RECHECK_TICKS] once synced data is applied. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        OdysseyBook.ensure(player)
        player.scheduler.runDelayed(plugin, { OdysseyBook.ensure(player) }, null, JOIN_RECHECK_TICKS)
    }

    /** Gives the respawned player the book on the next tick. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        player.scheduler.run(plugin, { OdysseyBook.ensure(player) }, null)
    }

    /** Gives the player the book after a world change. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        OdysseyBook.ensure(event.player)
    }

    /** Keeps the book out of the drops of a dying player. */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeath(event: PlayerDeathEvent) {
        event.drops.removeIf(OdysseyBook::isBook)
    }

    /** Cancels dropping the book. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        if (BookRules.isProtectedMove(BookMove.DROP, null, OdysseyBook.isBook(event.itemDrop.itemStack))) {
            event.isCancelled = true
        }
    }

    /** Cancels swapping the book between the hands. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onSwapHands(event: PlayerSwapHandItemsEvent) {
        val isBook = OdysseyBook.isBook(event.mainHandItem) || OdysseyBook.isBook(event.offHandItem)
        if (BookRules.isProtectedMove(BookMove.OFFHAND_SWAP, null, isBook)) event.isCancelled = true
    }

    /** Cancels every click that would move the book or touch its slot. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val slot = if (event.clickedInventory is PlayerInventory) event.slot else null
        val clickedBook = OdysseyBook.isBook(event.currentItem) || OdysseyBook.isBook(event.cursor)
        val move = when (event.click) {
            ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT -> BookMove.SHIFT_CLICK
            ClickType.DROP, ClickType.CONTROL_DROP -> BookMove.DROP
            else -> BookMove.CLICK
        }
        var protected = BookRules.isProtectedMove(move, slot, clickedBook) ||
                BookRules.isProtectedMove(BookMove.CLICK, slot, clickedBook)
        if (event.click == ClickType.NUMBER_KEY) {
            val hotbarBook = OdysseyBook.isBook(player.inventory.getItem(event.hotbarButton))
            protected = protected || BookRules.isProtectedMove(BookMove.NUMBER_KEY, event.hotbarButton, hotbarBook)
        }
        if (event.click == ClickType.SWAP_OFFHAND) {
            val offHandBook = OdysseyBook.isBook(player.inventory.itemInOffHand)
            protected = protected || BookRules.isProtectedMove(BookMove.OFFHAND_SWAP, null, clickedBook || offHandBook)
        }
        if (protected) event.isCancelled = true
    }

    /** Cancels dragging the book or dragging over its slot. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onDrag(event: InventoryDragEvent) {
        val isBook = OdysseyBook.isBook(event.oldCursor)
        val protected = event.rawSlots.any { raw ->
            val slot = if (event.view.getInventory(raw) is PlayerInventory) event.view.convertSlot(raw) else null
            BookRules.isProtectedMove(BookMove.DRAG, slot, isBook)
        }
        if (protected) event.isCancelled = true
    }

    /** Cancels putting the book into an item frame. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.rightClicked !is ItemFrame) return
        if (OdysseyBook.isBook(event.player.inventory.getItem(event.hand))) event.isCancelled = true
    }

    /** Cancels putting the book onto an armor stand. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onArmorStand(event: PlayerArmorStandManipulateEvent) {
        if (OdysseyBook.isBook(event.playerItem)) event.isCancelled = true
    }

    /** Opens the Odyssey menu on shift and right click with the book, or tells about research on a right click. */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(event: PlayerInteractEvent) {
        if (!OdysseyBook.isBook(event.item)) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        event.isCancelled = true
        if (event.hand != EquipmentSlot.HAND) return
        if (event.player.isSneaking) {
            OdysseyMenuDialog.show(event.player)
        } else {
            ProgressionMessages.researchSoon(event.player)
        }
    }

    /** The delays of the listener. */
    companion object {
        /** Ticks after a join until the book is checked once more. */
        const val JOIN_RECHECK_TICKS = 40L
    }
}
