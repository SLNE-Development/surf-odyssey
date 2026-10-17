package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import org.bukkit.World
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockFertilizeEvent
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.StructureGrowEvent

/**
 * Cancels every change of the hub world that is not a block break, as [EnvironmentRules] decide.
 *
 * Events in other worlds are left alone.
 */
object WorldChangeListener : Listener {
    private val hubWorld get() = HubPlugin.instance.hubWorld

    /** Cancels placing blocks. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) = apply(WorldChange.PLACE, event.block.world, event)

    /** Cancels emptying buckets. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) = apply(WorldChange.BUCKET_EMPTY, event.block.world, event)

    /** Cancels filling buckets. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBucketFill(event: PlayerBucketFillEvent) = apply(WorldChange.BUCKET_FILL, event.block.world, event)

    /** Keeps entity explosions from breaking blocks. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (!EnvironmentRules.allows(WorldChange.EXPLOSION, event.location.world == hubWorld)) event.blockList().clear()
    }

    /** Keeps block explosions from breaking blocks. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        if (!EnvironmentRules.allows(WorldChange.EXPLOSION, event.block.world == hubWorld)) event.blockList().clear()
    }

    /** Cancels fire burning blocks away. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBurn(event: BlockBurnEvent) = apply(WorldChange.BURN, event.block.world, event)

    /** Cancels blocks catching fire. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onIgnite(event: BlockIgniteEvent) = apply(WorldChange.IGNITE, event.block.world, event)

    /** Cancels fire, grass, mycelium and other blocks spreading. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onSpread(event: BlockSpreadEvent) = apply(WorldChange.SPREAD, event.block.world, event)

    /** Cancels snow, ice, concrete and other blocks forming, including those formed by entities. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onForm(event: BlockFormEvent) = apply(WorldChange.FORM, event.block.world, event)

    /** Cancels ice and snow melting, coral dying and other blocks fading. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFade(event: BlockFadeEvent) = apply(WorldChange.FADE, event.block.world, event)

    /** Cancels liquids flowing. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFluidFlow(event: BlockFromToEvent) = apply(WorldChange.FLUID_FLOW, event.block.world, event)

    /** Cancels pistons extending. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) = apply(WorldChange.PISTON, event.block.world, event)

    /** Cancels pistons retracting. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) = apply(WorldChange.PISTON, event.block.world, event)

    /** Cancels plants growing. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onGrow(event: BlockGrowEvent) = apply(WorldChange.GROW, event.block.world, event)

    /** Cancels saplings and mushrooms growing into structures. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) = apply(WorldChange.STRUCTURE_GROW, event.world, event)

    /** Cancels fertilizing blocks. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFertilize(event: BlockFertilizeEvent) = apply(WorldChange.FERTILIZE, event.block.world, event)

    /** Cancels leaf decay. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onDecay(event: LeavesDecayEvent) = apply(WorldChange.LEAF_DECAY, event.block.world, event)

    /** Cancels players trampling farmland. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPhysicalInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (EnvironmentRules.isTrample(event.action, block.type)) apply(WorldChange.TRAMPLE, block.world, event)
    }

    /** Cancels entities changing blocks, e.g. endermen, falling blocks or mobs trampling farmland. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityChange(event: EntityChangeBlockEvent) = apply(WorldChange.ENTITY_CHANGE, event.block.world, event)

    /** Cancels placing item frames, paintings and other hanging entities. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingPlace(event: HangingPlaceEvent) = apply(WorldChange.HANGING_PLACE, event.entity.world, event)

    /** Cancels breaking hanging entities, by players, other entities, explosions or physics. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingBreak(event: HangingBreakEvent) = apply(WorldChange.HANGING_BREAK, event.entity.world, event)

    /** Cancels players using decorations, e.g. rotating the item of an item frame. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        if (EnvironmentRules.isDecoration(entity.type)) apply(WorldChange.DECORATION_USE, entity.world, event)
    }

    /** Cancels players putting items into, rotating or taking items out of item frames. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onItemFrameChange(event: PlayerItemFrameChangeEvent) = apply(WorldChange.DECORATION_USE, event.itemFrame.world, event)

    /** Cancels players changing the equipment of armor stands. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) =
        apply(WorldChange.DECORATION_USE, event.rightClicked.world, event)

    /** Cancels damage to decorations, e.g. armor stands or the item of an item frame. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (EnvironmentRules.isDecoration(entity.type)) apply(WorldChange.DECORATION_DAMAGE, entity.world, event)
    }

    /** Cancels [event] unless [EnvironmentRules] allow [change] in [world]. */
    private fun apply(change: WorldChange, world: World, event: Cancellable) {
        if (!EnvironmentRules.allows(change, world == hubWorld)) event.isCancelled = true
    }
}
