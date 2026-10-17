package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.block.Action

/** Decides whether a [ProtectionCategory] of change may alter a world and classifies the events that cause them. */
object EnvironmentRules {
    private val HANGING = setOf(EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME, EntityType.PAINTING)

    /**
     * Returns whether a change of [category] may happen at a block. Outside the hub world every change
     * may happen. Inside it, the change may happen if the [world] settings allow it, or if the block
     * lies in a region whose [region] settings allow it; `null` means the block is in no region.
     */
    fun allows(category: ProtectionCategory, inHubWorld: Boolean, world: ProtectionSettings, region: ProtectionSettings?): Boolean =
        !inHubWorld || category.allowedIn(world) || (region != null && category.allowedIn(region))

    /** Returns whether an interaction of type [action] with a block of [material] tramples farmland. */
    fun isTrample(action: Action, material: Material): Boolean = action == Action.PHYSICAL && material == Material.FARMLAND

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
