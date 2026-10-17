package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

/**
 * A change to the blocks of a hub world other than a player breaking a block.
 *
 * @property byPlayer whether a player causes the change directly
 */
enum class WorldChange(val byPlayer: Boolean) {
    /** A player places a block. */
    PLACE(true),

    /** A player empties a bucket into the world. */
    BUCKET_EMPTY(true),

    /** A player fills a bucket from the world. */
    BUCKET_FILL(true),

    /** A player changes a block as an entity, e.g. by trampling it. */
    PLAYER_CHANGE(true),

    /** An entity or block explosion destroys blocks. */
    EXPLOSION(false),

    /** Fire burns a block away. */
    BURN(false),

    /** A block spreads to another, e.g. fire, grass or vines. */
    SPREAD(false),

    /** Leaves decay. */
    LEAF_DECAY(false),

    /** A non-player entity changes a block, e.g. an enderman or a falling block. */
    ENTITY_CHANGE(false),
}

/** Decides whether a [WorldChange] may alter a hub world. */
object EnvironmentRules {
    /**
     * Returns whether [change] may happen. Changes nobody causes directly are never allowed;
     * changes a player causes are allowed only while that player is [building].
     */
    fun allows(change: WorldChange, building: Boolean): Boolean = change.byPlayer && building
}
