package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.selection

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry
import dev.slne.surf.odyssey.runtime.paper.hub.plugin
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.command.Selections
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockPos
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.logging.Level
import kotlin.time.Duration.Companion.milliseconds

object SelectionWand : Listener {
    private val markerKey = NamespacedKey("odyssey", "skillworld_wand")
    private const val MAX_OUTLINE_POINTS = 400
    private const val REFRESH_MILLIS = 500L
    private val dust = Particle.DustOptions(Color.AQUA, 1f)

    private val lastFailureLog = java.util.concurrent.ConcurrentHashMap<Class<*>, Long>()

    @Volatile
    private var outlineJob: Job? = null

    fun createItem(): ItemStack = buildItem(Material.WOODEN_AXE, 1) {
        displayName { primary("Skillworld-Auswahl") }
        buildLore {
            line { info("Linksklick auf einen Block: erste Ecke") }
            line { info("Rechtsklick auf einen Block: zweite Ecke") }
        }
        editMeta { it.persistentDataContainer.set(markerKey, PersistentDataType.BOOLEAN, true) }
    }

    fun isWand(item: ItemStack?): Boolean =
        item != null && item.type == Material.WOODEN_AXE && item.persistentDataContainer.has(markerKey, PersistentDataType.BOOLEAN)

    fun give(player: Player) {
        if (player.inventory.contents.any { isWand(it) }) {
            player.sendText {
                appendErrorPrefix()
                error("Du besitzt bereits eine Skillworld-Auswahl.")
            }
            return
        }

        val leftover = player.inventory.addItem(createItem())
        if (leftover.isNotEmpty()) {
            player.sendText {
                appendErrorPrefix()
                error("Dein Inventar ist voll.")
            }
            return
        }

        player.sendText {
            appendSuccessPrefix()
            success("Du hast die ")
            variableValue("Skillworld-Auswahl")
            success(" erhalten.")
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val action = event.action
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (!isWand(event.item)) return
        val player = event.player
        if (!player.hasPermission(OdysseyBasePermissionRegistry.SKILL_WORLD_COMMAND)) return

        event.isCancelled = true
        val pos = BlockPos(block.x, block.y, block.z)
        val first = action == Action.LEFT_CLICK_BLOCK
        val (storedFirst, storedSecond) = Selections.peek(player)
        if (pos == (if (first) storedFirst else storedSecond)) return
        if (first) Selections.setFirst(player, pos) else Selections.setSecond(player, pos)

        player.sendText {
            appendSuccessPrefix()
            success(if (first) "Erste Ecke gesetzt: " else "Zweite Ecke gesetzt: ")
            variableValue("${pos.x} ${pos.y} ${pos.z}")
        }
    }

    @Synchronized
    fun startOutline() {
        if (outlineJob?.isActive == true) return

        outlineJob = plugin.scope.launch(plugin.globalRegionDispatcher) {
            while (isActive) {
                Bukkit.getOnlinePlayers().forEach { player ->
                    try {
                        if (isWand(player.inventory.itemInMainHand)) showOutline(player)
                    } catch (e: Exception) {
                        logFailure(e)
                    }
                }
                delay(REFRESH_MILLIS.milliseconds)
            }
        }
    }

    private fun logFailure(e: Exception) {
        val now = System.currentTimeMillis()
        val last = lastFailureLog[e.javaClass]
        if (last != null && now - last < 60_000L) return

        lastFailureLog[e.javaClass] = now
        plugin.logger.log(Level.SEVERE, "The selection outline failed", e)
    }

    @Synchronized
    fun stopOutline() {
        outlineJob?.cancel()
        outlineJob = null
    }

    private fun showOutline(player: Player) {
        val (first, second) = Selections.peek(player)
        val a = first ?: second ?: return
        val b = second ?: a

        selectionOutline(a, b, MAX_OUTLINE_POINTS).forEach {
            player.spawnParticle(Particle.DUST, it.x, it.y, it.z, 1, 0.0, 0.0, 0.0, 0.0, dust)
        }
    }
}
