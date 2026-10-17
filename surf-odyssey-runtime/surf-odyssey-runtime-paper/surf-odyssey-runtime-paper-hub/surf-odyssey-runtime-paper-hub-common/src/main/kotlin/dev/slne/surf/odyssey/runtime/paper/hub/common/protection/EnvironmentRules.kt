package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.block.Action

/** A change to a world other than a player breaking a block. */
enum class WorldChange {
    /** A player places a block. */
    PLACE,

    /** A player empties a bucket into the world. */
    BUCKET_EMPTY,

    /** A player fills a bucket from the world. */
    BUCKET_FILL,

    /** An entity or block explosion destroys blocks. */
    EXPLOSION,

    /** Fire burns a block away. */
    BURN,

    /** A block catches fire, e.g. from flint and steel, lava or lightning. */
    IGNITE,

    /** A block spreads to another, e.g. fire, grass, mycelium or vines. */
    SPREAD,

    /** A block forms, e.g. snow, ice or concrete. */
    FORM,

    /** A block fades, e.g. melting ice or snow or dying coral. */
    FADE,

    /** A liquid flows into another block. */
    FLUID_FLOW,

    /** A piston extends or retracts and moves blocks. */
    PISTON,

    /** A plant grows, e.g. crops, cactus or sugar cane. */
    GROW,

    /** A sapling or mushroom grows into a structure. */
    STRUCTURE_GROW,

    /** A block is fertilized, e.g. with bone meal. */
    FERTILIZE,

    /** Leaves decay. */
    LEAF_DECAY,

    /** A player tramples farmland. */
    TRAMPLE,

    /** An entity changes a block, e.g. an enderman, a falling block or a mob trampling farmland. */
    ENTITY_CHANGE,

    /** A hanging entity such as an item frame or a painting is placed. */
    HANGING_PLACE,

    /** A hanging entity such as an item frame or a painting is broken. */
    HANGING_BREAK,

    /** A player uses a decoration, e.g. rotates or takes the item of an item frame or changes an armor stand. */
    DECORATION_USE,

    /** A decoration such as an armor stand or an item frame takes damage. */
    DECORATION_DAMAGE,
}

/** Decides whether a [WorldChange] may alter a world and classifies the events that cause them. */
object EnvironmentRules {
    private val DECORATIONS = setOf(EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME, EntityType.PAINTING, EntityType.ARMOR_STAND)

    /** Returns whether [change] may happen; nothing changes in the hub world, other worlds are left alone. */
    fun allows(change: WorldChange, inHubWorld: Boolean): Boolean = !inHubWorld

    /** Returns whether an interaction of type [action] with a block of [material] tramples farmland. */
    fun isTrample(action: Action, material: Material): Boolean = action == Action.PHYSICAL && material == Material.FARMLAND

    /** Returns whether entities of [type] are decorations: item frames, paintings and armor stands. */
    fun isDecoration(type: EntityType): Boolean = type in DECORATIONS
}
