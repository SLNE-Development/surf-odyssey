package dev.slne.surf.odyssey.runtime.paper.progression.tool

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import kotlin.time.Duration.Companion.seconds

/**
 * Writes buffered tool usage to the tool items when they could leave the player's hand or inventory: on a switch of
 * the held item, a drop, opening or closing an inventory, death and quit.
 */
class ToolWriteListener : Listener {
    /** Writes the tool in the previously held slot. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onHeldItemChange(event: PlayerItemHeldEvent) {
        ToolUsage.flushSlot(event.player, event.previousSlot)
    }

    /** Writes a dropped tool before its item entity carries it away. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        val stack = event.itemDrop.itemStack
        if (ToolUsage.writeInto(stack)) event.itemDrop.itemStack = stack
    }

    /** Writes every tool of a player who opens an inventory, before items can be moved into it. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        (event.player as? Player)?.let(ToolUsage::flushAll)
    }

    /** Writes every tool of a player who closes an inventory. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        (event.player as? Player)?.let(ToolUsage::flushAll)
    }

    /** Writes the tools among the drops of a dying player and the tools the player keeps. */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onDeath(event: PlayerDeathEvent) {
        event.drops.forEach(ToolUsage::writeInto)
        ToolUsage.flushAll(event.player)
    }

    /** Writes every tool of a leaving player. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        ToolUsage.flushOnQuit(event.player)
    }

    /** Holds the background write loop. */
    companion object {
        /**
         * Writes, every second, the buffered usage of each tool whose last write is at least a second ago, on the
         * thread of its holder, while the holder is online and the plugin is enabled.
         *
         * @param plugin the plugin that owns the coroutines
         */
        fun startWriteLoop(plugin: Plugin) {
            plugin.launch {
                while (isActive) {
                    delay(1.seconds)
                    ToolUsage.due().groupBy({ it.second }, { it.first }).forEach { (holder, tools) ->
                        val player = plugin.server.getPlayer(holder) ?: return@forEach
                        plugin.launch {
                            withContext(plugin.entityDispatcher(player)) {
                                if (player.isOnline) tools.forEach { ToolUsage.flushTool(player, it) }
                            }
                        }
                    }
                }
            }
        }
    }
}
