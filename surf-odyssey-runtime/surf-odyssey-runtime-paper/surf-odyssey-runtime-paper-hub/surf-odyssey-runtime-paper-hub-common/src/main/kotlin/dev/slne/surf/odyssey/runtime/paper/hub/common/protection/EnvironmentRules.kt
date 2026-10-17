package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.block.Action

/** Decides whether a [ProtectionCategory] of change may alter a world and classifies the events that cause them. */
object EnvironmentRules {
    private val HANGING = setOf(EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME, EntityType.PAINTING)
    private val BOOKS = setOf(Material.WRITABLE_BOOK, Material.WRITTEN_BOOK)
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
     * Right-clicking doors, trapdoors, fence gates, buttons, levers and decoration blocks changes them;
     * a lectern changes only when a book is put on it. Stepping on a pressure plate triggers it.
     */
    fun blockUse(action: Action, material: Material, item: Material?): ProtectionCategory? = when (action) {
        Action.PHYSICAL -> if (material in PRESSURE_PLATES) ProtectionCategory.PRESSURE_PLATES else null
        Action.RIGHT_CLICK_BLOCK -> when {
            material in DOORS -> ProtectionCategory.DOORS
            material in BUTTONS -> ProtectionCategory.BUTTONS
            material == Material.LEVER -> ProtectionCategory.LEVERS
            material in DECO_BLOCKS -> ProtectionCategory.DECO_BLOCKS
            material == Material.LECTERN && item in BOOKS -> ProtectionCategory.DECO_BLOCKS
            else -> null
        }
        else -> null
    }

    /** Returns the category of entities of [type]: hanging entities, armor stands, or `null` for any other entity. */
    fun decorationCategory(type: EntityType): ProtectionCategory? = when (type) {
        in HANGING -> ProtectionCategory.HANGING_ENTITIES
        EntityType.ARMOR_STAND -> ProtectionCategory.ARMOR_STANDS
        else -> null
    }

    /** Returns the category of a block spreading from a block of [source]: fire for fire, spread otherwise. */
    fun spreadCategory(source: Material): ProtectionCategory =
        if (source == Material.FIRE || source == Material.SOUL_FIRE) ProtectionCategory.FIRE else ProtectionCategory.SPREAD
}
