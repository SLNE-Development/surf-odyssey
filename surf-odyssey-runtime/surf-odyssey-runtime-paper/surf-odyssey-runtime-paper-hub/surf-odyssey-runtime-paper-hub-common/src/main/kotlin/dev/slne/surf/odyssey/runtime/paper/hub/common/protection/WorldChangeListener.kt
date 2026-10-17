package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import io.papermc.paper.event.player.PlayerOpenSignEvent
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockFertilizeEvent
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.entity.EntityPlaceEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerTakeLecternBookEvent
import org.bukkit.event.world.StructureGrowEvent

/**
 * Cancels every change of the hub world that is not a block break, unless the world's or the
 * affected block's region's protection settings allow it, as [EnvironmentRules] decide. Also passes
 * block interactions to the region they happen in.
 *
 * Events in other worlds are left alone.
 */
object WorldChangeListener : Listener {
    private val hub get() = HubPlugin.instance

    /** Cancels placing blocks. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) = apply(ProtectionCategory.BLOCK_PLACE, event.block, event)

    /** Cancels emptying buckets. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) = apply(ProtectionCategory.BUCKETS, event.block, event)

    /** Cancels filling buckets. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBucketFill(event: PlayerBucketFillEvent) = apply(ProtectionCategory.BUCKETS, event.block, event)

    /** Keeps entity explosions from breaking blocks where explosions are not allowed. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().removeIf { !allows(ProtectionCategory.EXPLOSIONS, it) }
    }

    /** Keeps block explosions from breaking blocks where explosions are not allowed. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { !allows(ProtectionCategory.EXPLOSIONS, it) }
    }

    /** Cancels fire burning blocks away. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBurn(event: BlockBurnEvent) = apply(ProtectionCategory.FIRE, event.block, event)

    /** Cancels blocks catching fire. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onIgnite(event: BlockIgniteEvent) = apply(ProtectionCategory.FIRE, event.block, event)

    /** Cancels fire, grass, mycelium and other blocks spreading. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onSpread(event: BlockSpreadEvent) = apply(EnvironmentRules.spreadCategory(event.source.type), event.block, event)

    /** Cancels snow, ice, concrete and other blocks forming, including those formed by entities. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onForm(event: BlockFormEvent) = apply(ProtectionCategory.FORM, event.block, event)

    /** Cancels ice and snow melting, coral dying and other blocks fading. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFade(event: BlockFadeEvent) = apply(ProtectionCategory.FADE, event.block, event)

    /** Cancels liquids flowing into a block. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFluidFlow(event: BlockFromToEvent) = apply(ProtectionCategory.FLUID_FLOW, event.toBlock, event)

    /** Cancels pistons extending. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) = applyPiston(event, event.blocks)

    /** Cancels pistons retracting. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) = applyPiston(event, event.blocks)

    /** Cancels plants growing. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onGrow(event: BlockGrowEvent) = apply(ProtectionCategory.GROW, event.block, event)

    /**
     * Cancels saplings and mushrooms growing into structures where growing is not allowed, and keeps
     * an allowed structure from placing blocks where growing is not allowed.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) {
        if (!allows(ProtectionCategory.GROW, event.location)) {
            event.isCancelled = true
            return
        }
        event.blocks.removeIf { !allows(ProtectionCategory.GROW, it.world, it.x, it.y, it.z) }
    }

    /** Cancels fertilizing blocks. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFertilize(event: BlockFertilizeEvent) = apply(ProtectionCategory.FERTILIZE, event.block, event)

    /** Cancels leaf decay. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onDecay(event: LeavesDecayEvent) = apply(ProtectionCategory.LEAF_DECAY, event.block, event)

    /**
     * Denies trampling farmland and using doors, buttons, pressure plates and the other blocks
     * [EnvironmentRules.interactionCategory] classifies, where their category is not allowed, then
     * passes the interaction to the region of the clicked block in the hub world, also when the use
     * was denied. A denied block use still lets the player use the item in their hand.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        val category = EnvironmentRules.interactionCategory(event.action, block.type, event.item?.type)
        if (category != null && !allows(category, block)) event.setUseInteractedBlock(Event.Result.DENY)
        if (block.world == hub.hubWorld) hub.regionIndex.regionAt(block.x, block.y, block.z)?.onInteract(event)
    }

    /** Cancels mobs and other entities triggering pressure plates, tripwires and other blocks by stepping on them. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityInteract(event: EntityInteractEvent) {
        val category = EnvironmentRules.entityInteractionCategory(event.block.type) ?: return
        apply(category, event.block, event)
    }

    /** Cancels entities changing blocks, e.g. endermen, falling blocks or mobs trampling farmland. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityChange(event: EntityChangeBlockEvent) = apply(ProtectionCategory.ENTITY_BLOCK_CHANGES, event.block, event)

    /** Cancels placing item frames, paintings and other hanging entities. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingPlace(event: HangingPlaceEvent) = apply(ProtectionCategory.HANGING_ENTITIES, event.entity.location, event)

    /** Cancels breaking hanging entities, by players, other entities, explosions or physics. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingBreak(event: HangingBreakEvent) = apply(ProtectionCategory.HANGING_ENTITIES, event.entity.location, event)

    /** Cancels players using item frames, paintings and armor stands, e.g. rotating the item of an item frame. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        val category = EnvironmentRules.decorationCategory(entity.type) ?: return
        apply(category, entity.location, event)
    }

    /** Cancels players putting items into, rotating or taking items out of item frames. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onItemFrameChange(event: PlayerItemFrameChangeEvent) =
        apply(ProtectionCategory.HANGING_ENTITIES, event.itemFrame.location, event)

    /** Cancels players placing boats, minecarts, end crystals and armor stands. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityPlace(event: EntityPlaceEvent) =
        apply(EnvironmentRules.placeCategory(event.entity.type), event.entity.location, event)

    /** Cancels players changing the equipment of armor stands. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) =
        apply(ProtectionCategory.ARMOR_STANDS, event.rightClicked.location, event)

    /** Cancels damage to item frames, paintings and armor stands. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        val entity = event.entity
        val category = EnvironmentRules.decorationCategory(entity.type) ?: return
        apply(category, entity.location, event)
    }

    /** Cancels players opening the sign editor. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onOpenSign(event: PlayerOpenSignEvent) = apply(ProtectionCategory.SIGNS, event.sign.block, event)

    /** Cancels players changing the text of a sign. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onSignChange(event: SignChangeEvent) = apply(ProtectionCategory.SIGNS, event.block, event)

    /** Cancels dispensers and droppers dispensing items. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onDispense(event: BlockDispenseEvent) = apply(ProtectionCategory.DISPENSERS, event.block, event)

    /** Cancels players taking the book from a lectern. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onTakeLecternBook(event: PlayerTakeLecternBookEvent) = apply(ProtectionCategory.DECO_BLOCKS, event.lectern.block, event)

    /** Cancels [event] unless the piston and every block it moves, before and after moving, allow pistons. */
    private fun applyPiston(event: BlockPistonEvent, moved: List<Block>) {
        val direction = event.direction
        val allowed = allows(ProtectionCategory.PISTONS, event.block) &&
            moved.all { allows(ProtectionCategory.PISTONS, it) && allows(ProtectionCategory.PISTONS, it.getRelative(direction)) }
        if (!allowed) event.isCancelled = true
    }

    /** Cancels [event] unless [category] is allowed at [block]. */
    private fun apply(category: ProtectionCategory, block: Block, event: Cancellable) {
        if (!allows(category, block)) event.isCancelled = true
    }

    /** Cancels [event] unless [category] is allowed at the block of [location]. */
    private fun apply(category: ProtectionCategory, location: Location, event: Cancellable) {
        if (!allows(category, location)) event.isCancelled = true
    }

    /** Returns whether [category] is allowed at [block]. */
    private fun allows(category: ProtectionCategory, block: Block): Boolean = allows(category, block.world, block.x, block.y, block.z)

    /** Returns whether [category] is allowed at the block of [location]. */
    private fun allows(category: ProtectionCategory, location: Location): Boolean =
        allows(category, location.world, location.blockX, location.blockY, location.blockZ)

    /**
     * Returns whether [category] is allowed at the block [x], [y], [z] of [world]: always outside the
     * hub world, otherwise when the world's settings or the settings of the region covering the block allow it.
     * The region is looked up only when the world's settings do not allow [category].
     */
    private fun allows(category: ProtectionCategory, world: World?, x: Int, y: Int, z: Int): Boolean {
        val hub = hub
        if (world != hub.hubWorld || category.allowedIn(hub.protectionSettings)) return true
        return EnvironmentRules.allows(category, true, hub.protectionSettings, hub.regionIndex.regionAt(x, y, z)?.settings)
    }
}
