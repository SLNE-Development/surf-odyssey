package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.block.Action

/** Decides whether a [ProtectionCategory] of change may alter a world and classifies the events that cause them. */
object EnvironmentRules {
    private val HANGING = setOf(EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME, EntityType.PAINTING)
    private val BOOKS = setOf(Material.WRITABLE_BOOK, Material.WRITTEN_BOOK)
    private val REDSTONE_ORES = setOf(Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE)
    private val REDSTONE_CLICKED = setOf(Material.NOTE_BLOCK, Material.REPEATER, Material.COMPARATOR, Material.DAYLIGHT_DETECTOR) + REDSTONE_ORES
    private val REDSTONE_STEPPED = setOf(Material.TRIPWIRE) + REDSTONE_ORES
    private val BERRIES = setOf(Material.SWEET_BERRY_BUSH, Material.CAVE_VINES, Material.CAVE_VINES_PLANT)
    private val PLANTS_STEPPED = setOf(Material.BIG_DRIPLEAF, Material.TURTLE_EGG)
    private val UTILITY_BLOCKS = setOf(
        Material.JUKEBOX, Material.BELL, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CAULDRON,
        Material.WATER_CAULDRON, Material.LAVA_CAULDRON, Material.POWDER_SNOW_CAULDRON, Material.BEEHIVE,
        Material.BEE_NEST, Material.RESPAWN_ANCHOR, Material.END_PORTAL_FRAME,
    )
    private val BLOCKS = Material.entries.filterNot { it.name.startsWith("LEGACY_") }

    private val DOORS = BLOCKS.filterTo(mutableSetOf()) {
        it.name.endsWith("_DOOR") || it.name.endsWith("_TRAPDOOR") || it.name.endsWith("_FENCE_GATE")
    }
    private val BUTTONS = BLOCKS.filterTo(mutableSetOf()) { it.name.endsWith("_BUTTON") }
    private val PRESSURE_PLATES = BLOCKS.filterTo(mutableSetOf()) { it.name.endsWith("_PRESSURE_PLATE") }
    private val DECO_BLOCKS = BLOCKS.filterTo(mutableSetOf()) {
        it == Material.FLOWER_POT || it.name.startsWith("POTTED_") || it == Material.CAKE ||
            it == Material.CANDLE || it.name.endsWith("_CANDLE") || it.name.endsWith("CANDLE_CAKE") ||
            it == Material.COMPOSTER || it == Material.CHISELED_BOOKSHELF || it == Material.DECORATED_POT
    }

    /**
     * Returns whether a change of [category] may happen at a block. Outside the hub world every change
     * may happen. Inside it, the change may happen if the [world] settings allow it, or if the block
     * lies in a region whose [region] settings allow it; `null` means the block is in no region.
     */
    fun allows(category: ProtectionCategory, inHubWorld: Boolean, world: ProtectionSettings, region: ProtectionSettings?): Boolean =
        !inHubWorld || category.allowedIn(world) || (region != null && category.allowedIn(region))

    /** Returns whether an interaction of type [action] with a block of [material] tramples farmland. */
    fun isTrample(action: Action, material: Material): Boolean = action == Action.PHYSICAL && material == Material.FARMLAND

    /**
     * Returns the category of using a block of [material] through an interaction of type [action] while
     * holding an item of type [item] (`null` for an empty hand), or `null` if the interaction does not
     * change the block.
     *
     * Right-clicking doors, trapdoors, fence gates, buttons, levers, decoration blocks, note blocks,
     * repeaters, comparators, daylight detectors, redstone ore and utility blocks such as jukeboxes,
     * bells, campfires, cauldrons or beehives changes or uses them, and right-clicking berry bushes or
     * cave vines picks their berries; a lectern changes only when a book is put on it. Stepping on a
     * pressure plate, a tripwire or redstone ore triggers it, stepping on a big dripleaf tilts it and
     * stepping on turtle eggs tramples them.
     * Left-clicks never count, so breaking a block is not affected.
     */
    fun blockUse(action: Action, material: Material, item: Material?): ProtectionCategory? = when (action) {
        Action.PHYSICAL -> physicalUse(material)
        Action.RIGHT_CLICK_BLOCK -> rightClickUse(material, item)
        else -> null
    }

    /** Returns the category of stepping on a block of [material], or `null` if that changes nothing protected. */
    private fun physicalUse(material: Material): ProtectionCategory? = when (material) {
        in PRESSURE_PLATES -> ProtectionCategory.PRESSURE_PLATES
        in REDSTONE_STEPPED -> ProtectionCategory.REDSTONE_COMPONENTS
        in PLANTS_STEPPED -> ProtectionCategory.PLANT_HARVEST
        else -> null
    }

    /**
     * Returns the category of right-clicking a block of [material] while holding an item of type [item],
     * or `null` if that changes nothing protected.
     */
    private fun rightClickUse(material: Material, item: Material?): ProtectionCategory? = when {
        material in DOORS -> ProtectionCategory.DOORS
        material in BUTTONS -> ProtectionCategory.BUTTONS
        material == Material.LEVER -> ProtectionCategory.LEVERS
        material in DECO_BLOCKS -> ProtectionCategory.DECO_BLOCKS
        material == Material.LECTERN && item in BOOKS -> ProtectionCategory.DECO_BLOCKS
        material in REDSTONE_CLICKED -> ProtectionCategory.REDSTONE_COMPONENTS
        material in UTILITY_BLOCKS -> ProtectionCategory.UTILITY_BLOCKS
        material in BERRIES -> ProtectionCategory.PLANT_HARVEST
        else -> null
    }

    /**
     * Returns the category of a player interacting with a block of [material] through an interaction
     * of type [action] while holding an item of type [item]: [ProtectionCategory.TRAMPLE] for trampling
     * farmland, otherwise the [blockUse] category, or `null` if the interaction changes nothing protected.
     */
    fun interactionCategory(action: Action, material: Material, item: Material?): ProtectionCategory? =
        if (isTrample(action, material)) ProtectionCategory.TRAMPLE else blockUse(action, material, item)

    /** Returns the category of entities of [type]: hanging entities, armor stands, or `null` for any other entity. */
    fun decorationCategory(type: EntityType): ProtectionCategory? = when (type) {
        in HANGING -> ProtectionCategory.HANGING_ENTITIES
        EntityType.ARMOR_STAND -> ProtectionCategory.ARMOR_STANDS
        else -> null
    }

    /** Returns the category of a player placing an entity of [type]: armor stands for armor stands, vehicles otherwise. */
    fun placeCategory(type: EntityType): ProtectionCategory =
        if (type == EntityType.ARMOR_STAND) ProtectionCategory.ARMOR_STANDS else ProtectionCategory.VEHICLES

    /** Returns the category of a block spreading from a block of [source]: fire for fire, spread otherwise. */
    fun spreadCategory(source: Material): ProtectionCategory =
        if (source == Material.FIRE || source == Material.SOUL_FIRE) ProtectionCategory.FIRE else ProtectionCategory.SPREAD
}
