package dev.slne.surf.odyssey.runtime.paper.progression.definition.station

import dev.slne.surf.odyssey.runtime.paper.progression.definition.Definition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.ProgressionDsl
import net.kyori.adventure.text.Component
import org.bukkit.Material

/**
 * A kind of crafting station. Recipes belong to a station kind, not to a position in a world.
 *
 * @property id stable station ID
 * @property displayName name shown in the station menu
 * @property icon material shown for the station in menus
 */
class StationDefinition(
    override val id: String,
    val displayName: Component,
    val icon: Material,
) : Definition {
    /** Returns [id] for log and validation messages. */
    override fun toString(): String = "StationDefinition($id)"
}

/**
 * Builds the values of a [StationDefinition].
 *
 * @property id the station ID
 */
@ProgressionDsl
class StationBuilder(private val id: String) {
    /** Name shown in the station menu; required. */
    var displayName: Component? = null

    /** Material shown for the station in menus; required. */
    var icon: Material? = null

    /**
     * Creates the definition.
     *
     * @throws IllegalStateException if [displayName] or [icon] is missing
     */
    fun build(): StationDefinition = StationDefinition(
        id = id,
        displayName = checkNotNull(displayName) { "station $id needs a displayName" },
        icon = checkNotNull(icon) { "station $id needs an icon" },
    )
}

/**
 * Defines a crafting station kind.
 *
 * @param id stable station ID
 * @param block sets the values of the station
 * @return the station definition
 */
fun station(id: String, block: StationBuilder.() -> Unit): StationDefinition =
    StationBuilder(id).apply(block).build()
