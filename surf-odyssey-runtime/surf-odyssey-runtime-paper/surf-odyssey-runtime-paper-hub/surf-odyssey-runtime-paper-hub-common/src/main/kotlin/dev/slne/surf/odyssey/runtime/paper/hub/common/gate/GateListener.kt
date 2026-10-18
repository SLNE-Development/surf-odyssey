package dev.slne.surf.odyssey.runtime.paper.hub.common.gate

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.PlayerRegionTracker
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Cancels what players do in regions whose gate locks them out: breaking and placing blocks, interacting with blocks
 * and entities, fishing, attacking and picking up items. An action counts as inside a locked region if the player stands in one or the
 * block, entity or hook it targets lies in one. The player is reminded in the action bar, except when stepping on
 * blocks, clicking into the air or picking up items.
 *
 * Every handler runs first, so cancelled actions give no drops, quest progress or tool usage.
 */
object GateListener : Listener {
    /** Cancels breaking a block in a locked region. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (blocks(event.player, event.block.location)) event.isCancelled = true
    }

    /** Cancels placing a block in a locked region. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        if (blocks(event.player, event.block.location)) event.isCancelled = true
    }

    /**
     * Denies using a block or the held item in a locked region. Also handles interactions that arrive cancelled, since
     * clicks into the air always do.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(event: PlayerInteractEvent) {
        if (!isLockedOut(event.player, event.clickedBlock?.location)) return
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
        if (remindsOn(event.action)) GateAccess.tellRegionLocked(event.player)
    }

    /**
     * Returns whether a denied interaction of kind [action] reminds the player of the locked region: only clicks on
     * blocks do.
     *
     * @param action the kind of interaction
     */
    fun remindsOn(action: Action): Boolean = action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK

    /** Cancels interacting with an entity in a locked region. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        if (blocks(event.player, event.rightClicked.location)) event.isCancelled = true
    }

    /** Cancels casting and catching in a locked region. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        if (event.state == PlayerFishEvent.State.REEL_IN || event.state == PlayerFishEvent.State.FAILED_ATTEMPT) return
        if (blocks(event.player, event.hook.location)) event.isCancelled = true
    }

    /** Cancels a player's melee or projectile attack in a locked region. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        } ?: return
        if (blocks(attacker, event.entity.location)) event.isCancelled = true
    }

    /**
     * Cancels a player picking up an item while standing in a locked region or from one, without a reminder, since
     * the event repeats every tick.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        if (isLockedOut(player, event.item.location)) event.isCancelled = true
    }

    /** Forgets the gate state of a leaving player. */
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) = GateEnforcer.forget(event.player)

    /**
     * Returns whether [player] is locked out of the region they stand in or the region at [target], and reminds them
     * if so.
     */
    private fun blocks(player: Player, target: Location?): Boolean {
        val locked = isLockedOut(player, target)
        if (locked) GateAccess.tellRegionLocked(player)
        return locked
    }

    /** Returns whether [player] is locked out of the region they stand in or the region at [target]. */
    private fun isLockedOut(player: Player, target: Location?): Boolean =
        GateAccess.isLockedOut(player, PlayerRegionTracker.regionOf(player)) ||
            (target != null && GateAccess.isLockedOut(player, GateAccess.regionAt(target)))
}
