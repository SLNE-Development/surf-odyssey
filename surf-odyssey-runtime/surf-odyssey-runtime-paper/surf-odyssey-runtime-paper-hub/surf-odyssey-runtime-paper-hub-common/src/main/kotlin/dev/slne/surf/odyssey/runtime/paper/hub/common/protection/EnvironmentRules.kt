package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

/** A change to the blocks of a hub world other than a player breaking a block. */
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

    /** A block spreads to another, e.g. fire, grass or vines. */
    SPREAD,

    /** Leaves decay. */
    LEAF_DECAY,

    /** An entity changes a block, e.g. an enderman or a falling block. */
    ENTITY_CHANGE,
}

/** Decides whether a [WorldChange] may alter a hub world. */
object EnvironmentRules {
    /** Returns whether [change] may happen. No change is allowed. */
    fun allows(change: WorldChange): Boolean = false
}
