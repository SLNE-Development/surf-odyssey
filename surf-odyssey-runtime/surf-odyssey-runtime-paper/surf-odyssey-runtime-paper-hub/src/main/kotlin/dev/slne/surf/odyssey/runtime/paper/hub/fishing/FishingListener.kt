package dev.slne.surf.odyssey.runtime.paper.hub.fishing

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.github.benmanes.caffeine.cache.Caffeine
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.odyssey.runtime.paper.hub.plugin
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.FishHook
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * Puts an item display on every fishing hook a player casts and removes it together with the hook.
 *
 * The hook removes itself whenever the player stops holding a rod (switching slots, dropping it, moving it into a
 * container, quitting, reeling in, moving too far away), so its removal is the single place the display is cleaned up.
 */
object FishingListener : Listener {
    data class Bobber(
        val itemStack: ItemStack,
        val bobberEntity: FishHook,
        val itemDisplayEntity: ItemDisplay
    )

    private val fishingRodCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .removalListener<UUID, Bobber> { uuid, bobber, cause ->
            server.sendPlainMessage("Fishing rod for player $uuid was removed from cache due to $cause")

            // Removed independently, so a hook that is already gone cannot keep the display alive
            bobber?.bobberEntity?.removeOnOwnThread()
            bobber?.itemDisplayEntity?.removeOnOwnThread()
        }
        .build<UUID, Bobber>()

    /** Runs last, so the display is only prepared for casts no other listener cancelled. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerFish(event: PlayerFishEvent) {
        if (event.state == PlayerFishEvent.State.FISHING) {
            handleThrown(event)
        }

        server.sendPlainMessage(event.state.name)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        fishingRodCache.invalidate(event.player.uniqueId)
    }

    @EventHandler
    fun onHookRemoved(event: EntityRemoveFromWorldEvent) {
        val hook = event.entity as? FishHook ?: return
        val owner = hook.shooter as? Player ?: return

        if (fishingRodCache.getIfPresent(owner.uniqueId)?.bobberEntity == hook) {
            fishingRodCache.invalidate(owner.uniqueId)
        }
    }

    private fun handleThrown(event: PlayerFishEvent) {
        val player = event.player
        val hook = event.hook
        val itemStackInHand = player.equipment.getItem(event.hand ?: EquipmentSlot.HAND).clone()

        // The hook is not in the world yet and can still be cancelled through ProjectileLaunchEvent. The entity
        // scheduler only runs this once the hook exists, so a cancelled cast never spawns a display without a hook.
        hook.scheduler.run(plugin, {
            if (!hook.isValid) return@run

            val display = summonDisplayEntity(hook)
            hook.addPassenger(display)

            fishingRodCache.put(player.uniqueId, Bobber(itemStackInHand, hook, display))
        }, null)
    }

    private fun summonDisplayEntity(hook: FishHook): ItemDisplay {
        return hook.world.spawnEntity(
            hook.location,
            EntityType.ITEM_DISPLAY,
            CreatureSpawnEvent.SpawnReason.CUSTOM
        ) { entity ->
            val itemDisplay = entity as ItemDisplay

            // Never saved with the chunk, so a restart or chunk unload cannot leave it behind
            itemDisplay.isPersistent = false
            itemDisplay.setRotation(0f, 0f)

            itemDisplay.setItemStack(buildItem(Material.DIAMOND, 1) {})
        } as ItemDisplay
    }

    /** Removes the entity on the thread that owns it; does nothing when it is already gone. */
    private fun Entity.removeOnOwnThread() {
        if (!isValid) return

        scheduler.run(plugin, { remove() }, null)
    }
}
