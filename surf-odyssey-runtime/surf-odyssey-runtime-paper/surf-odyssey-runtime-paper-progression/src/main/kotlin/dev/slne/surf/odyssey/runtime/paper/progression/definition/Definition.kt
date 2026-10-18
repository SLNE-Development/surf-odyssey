package dev.slne.surf.odyssey.runtime.paper.progression.definition

/**
 * A piece of progression content defined in code, such as an item, recipe, quest or NPC.
 *
 * Definitions are identified by a stable [id] that is unique within its kind and matches [ID_PATTERN].
 */
interface Definition {
    /** Stable identifier of this definition, unique within its kind. */
    val id: String

    /** Holds the pattern every definition ID has to match. */
    companion object {
        /** Pattern every definition ID has to match: lowercase letters, digits and underscores. */
        val ID_PATTERN: Regex = Regex("[a-z0-9_]+")
    }
}

/** Marks the builder classes of the progression definition DSL so their scopes do not leak into each other. */
@DslMarker
annotation class ProgressionDsl
