package dev.slne.surf.odyssey.runtime.paper.content

import dev.slne.surf.odyssey.runtime.paper.progression.definition.title.TitleDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.title.title
import net.kyori.adventure.text.Component.text

/** The titles of the Odyssey content. */
object Titles {
    /** "Hüter des Leuchtfeuers", granted for completing the lighthouse questline. */
    val LIGHTKEEPER: TitleDefinition = title("lightkeeper") {
        display = text("Hüter des Leuchtfeuers")
    }

    /** "Angler vom Ostkai", granted by a monthly quest. */
    val ANGLER: TitleDefinition = title("angler") {
        display = text("Angler vom Ostkai")
    }

    /** All titles in the order they are registered. */
    val all: List<TitleDefinition> = listOf(LIGHTKEEPER, ANGLER)
}
