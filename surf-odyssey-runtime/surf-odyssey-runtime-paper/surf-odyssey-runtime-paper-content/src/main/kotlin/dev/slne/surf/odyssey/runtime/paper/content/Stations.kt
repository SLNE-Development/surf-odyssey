package dev.slne.surf.odyssey.runtime.paper.content

import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.station
import net.kyori.adventure.text.Component.text
import org.bukkit.Material

/** The crafting station kinds of the Odyssey content. */
object Stations {
    /** The workbench, a block in the lobby, where tools and materials are made. */
    val WORKBENCH: StationDefinition = station("workbench") {
        displayName = text("Werkbank")
        icon = Material.CRAFTING_TABLE
    }

    /** The cooking station operated by the cook, where dishes are made. */
    val COOKING_STATION: StationDefinition = station("cooking_station") {
        displayName = text("Kochstelle")
        icon = Material.SMOKER
    }

    /** All stations in the order they are registered. */
    val all: List<StationDefinition> = listOf(WORKBENCH, COOKING_STATION)
}
