package dev.slne.surf.odyssey.runtime.paper.hub.south

import dev.slne.surf.odyssey.runtime.paper.content.Npcs
import dev.slne.surf.odyssey.runtime.paper.content.OdysseyContent
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.shape
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.Portal
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionPlacements
import dev.slne.surf.odyssey.runtime.paper.progression.SpawnPoint
import net.kyori.adventure.text.Component

/** Plugin of the South hub world, which has no regions yet and is home to Ysa. */
class SouthHubPlugin : HubPlugin() {
    /** Shown to players as "Süden". */
    override val worldDisplayName: Component = Component.text("Süden")

    /** The progression content of the network. */
    override val content = OdysseyContent

    /** Declares South's portals. */
    override fun portals() = southPortals()

    /** Places Ysa. */
    override fun ProgressionPlacements.placements() = southPlacements()
}

/** The portals of South: one back to the lobby. Coordinates are placeholders until the map is in place. */
fun southPortals() = listOf(
    Portal("to_lobby", shape { cuboid(-2, 64, -2, 2, 67, -2) }, "odyssey-hub-lobby", WorldPosition(0.5, 64.0, 0.5, 0f, 0f)),
)

/** Places Ysa on the spawn platform west of the spawn, facing it. */
fun ProgressionPlacements.southPlacements() {
    npc(Npcs.YSA, SpawnPoint(-3.5, 64.0, 5.5, -90f, 0f))
}
