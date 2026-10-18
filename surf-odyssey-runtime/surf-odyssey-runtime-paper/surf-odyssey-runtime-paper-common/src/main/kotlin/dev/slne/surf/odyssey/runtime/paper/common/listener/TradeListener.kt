package dev.slne.surf.odyssey.runtime.paper.common.listener

import dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view.forgetTradeHistory
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeManager
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradePreferences
import kotlinx.coroutines.runBlocking
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.logging.Level

object TradeListener : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val session = TradeManager.sessionOf(player.uniqueId) ?: return
        val side = session.sideOf(player.uniqueId) ?: return
        val context = side.context ?: return

        val clicked = event.clickedInventory ?: return
        if (clicked === event.view.topInventory) return

        event.isCancelled = true
        if (clicked !== player.inventory) return
        if (!event.isLeftClick && !event.isRightClick) return

        val stack = event.currentItem
        if (stack == null || stack.type == Material.AIR || stack.isEmpty) return

        val amount = if (event.isRightClick) 1 else stack.amount
        val moved = stack.clone().also { it.amount = amount }

        if (!session.addItem(player.uniqueId, moved)) {
            player.playNoSound()
            return
        }

        val remaining = stack.amount - amount
        clicked.setItem(event.slot, if (remaining <= 0) null else stack.clone().also { it.amount = remaining })
        context.update()
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        val side = TradeManager.sessionOf(player.uniqueId)?.sideOf(player.uniqueId) ?: return
        if (side.context == null) return

        event.isCancelled = true
    }

    /**
     * Loads the stored trade request setting of the joining player. A failure is logged and the player keeps the
     * default of accepting requests, so trading never blocks a login.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAsyncPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return

        runCatching { runBlocking { TradePreferences.load(event.uniqueId) } }
            .onFailure { plugin.logger.log(Level.SEVERE, "Failed to load the trade settings of ${event.uniqueId}", it) }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        TradeManager.forget(event.player)
        TradePreferences.evict(event.player.uniqueId)
        forgetTradeHistory(event.player.uniqueId)
    }
}
