package dev.slne.surf.odyssey.runtime.paper.hub.south

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.shape
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.Portal
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition
import net.kyori.adventure.text.Component

/** Plugin of the South hub world, which has no regions yet. */
class SouthHubPlugin : HubPlugin() {
    /** Shown to players as "Süden". */
    override val worldDisplayName: Component = Component.text("Süden")

    /** Declares South's portals: one back to the lobby. Coordinates are placeholders until the map is in place. */
    override fun portals() = listOf(
        Portal("to_lobby", shape { cuboid(-2, 64, -2, 2, 67, -2) }, "odyssey-hub-lobby", WorldPosition(0.5, 64.0, 0.5, 0f, 0f)),
    )
}
