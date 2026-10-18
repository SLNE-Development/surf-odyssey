package dev.slne.surf.odyssey.runtime.paper.progression.definition.title

import dev.slne.surf.odyssey.runtime.paper.progression.definition.Definition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.ProgressionDsl
import net.kyori.adventure.text.Component

/**
 * A title a player can earn and display.
 *
 * @property id stable title ID
 * @property display the title as shown next to the player name
 */
class TitleDefinition(override val id: String, val display: Component) : Definition {
    /** Returns [id] for log and validation messages. */
    override fun toString(): String = "TitleDefinition($id)"
}

/**
 * Builds the values of a [TitleDefinition].
 *
 * @property id the title ID
 */
@ProgressionDsl
class TitleBuilder(private val id: String) {
    /** The title as shown next to the player name; required. */
    var display: Component? = null

    /**
     * Creates the definition.
     *
     * @throws IllegalStateException if [display] is missing
     */
    fun build(): TitleDefinition = TitleDefinition(id, checkNotNull(display) { "title $id needs a display" })
}

/**
 * Defines a title.
 *
 * @param id stable title ID
 * @param block sets the display of the title
 * @return the title definition
 */
fun title(id: String, block: TitleBuilder.() -> Unit): TitleDefinition = TitleBuilder(id).apply(block).build()
