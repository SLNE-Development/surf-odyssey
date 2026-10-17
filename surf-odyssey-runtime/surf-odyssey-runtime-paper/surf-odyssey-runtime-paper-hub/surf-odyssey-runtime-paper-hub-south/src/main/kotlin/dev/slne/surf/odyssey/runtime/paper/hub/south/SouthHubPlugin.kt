package dev.slne.surf.odyssey.runtime.paper.hub.south

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import net.kyori.adventure.text.Component

/** Plugin of the South hub world, which has no regions yet. */
class SouthHubPlugin : HubPlugin() {
    /** Shown to players as "Süden". */
    override val worldDisplayName: Component = Component.text("Süden")
}
