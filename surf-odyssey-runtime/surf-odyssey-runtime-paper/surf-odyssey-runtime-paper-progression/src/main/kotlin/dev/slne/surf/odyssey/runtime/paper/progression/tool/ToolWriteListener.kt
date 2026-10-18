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
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import kotlin.time.Duration.Companion.seconds

/**
 * Writes buffered tool usage to the tool items when they could leave the player's hand or inventory: on a switch of
 * the held item, a drop, an interaction with an entity such as an item frame or armour stand, opening or closing an
 * inventory, death and quit.
 *
 * @param plugin the plugin whose log records dropped changes
 */
class ToolWriteListener(private val plugin: Plugin) : Listener {
    /** Writes the tool in the previously held slot. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onHeldItemChange(event: PlayerItemHeldEvent) {
        val tool = ToolItems.uuidOf(event.player.inventory.getItem(event.previousSlot)) ?: return
        ToolUsage.flushTool(event.player, tool)
    }

    /** Writes a dropped tool before its item entity carries it away. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        val stack = event.itemDrop.itemStack
        if (ToolUsage.writeInto(event.player.uniqueId, stack)) event.itemDrop.itemStack = stack
    }

    /** Writes the tool in the main hand before an entity such as an item frame or armour stand can take it. */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        val tool = ToolItems.uuidOf(event.player.inventory.itemInMainHand) ?: return
        ToolUsage.flushTool(event.player, tool)
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
        event.drops.forEach { ToolUsage.writeInto(event.player.uniqueId, it) }
        ToolUsage.flushAll(event.player)
    }

    /** Writes every tool of a leaving player and logs buffered changes whose tool was no longer held. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        val dropped = ToolUsage.flushOnQuit(event.player)
        dropped.forEach { tool ->
            plugin.logger.warning("Dropped buffered usage of ${event.player.uniqueId} for tool $tool, which was no longer held")
        }
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
                    ToolUsage.due().groupBy({ it.first }, { it.second }).forEach { (holder, tools) ->
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
