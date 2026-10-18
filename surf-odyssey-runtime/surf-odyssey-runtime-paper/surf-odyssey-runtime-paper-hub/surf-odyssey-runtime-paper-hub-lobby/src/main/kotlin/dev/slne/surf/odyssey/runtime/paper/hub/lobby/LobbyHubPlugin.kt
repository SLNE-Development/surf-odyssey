package dev.slne.surf.odyssey.runtime.paper.hub.lobby

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.content.Npcs
import dev.slne.surf.odyssey.runtime.paper.content.OdysseyContent
import dev.slne.surf.odyssey.runtime.paper.content.Stations
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.shape
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.Portal
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition
import dev.slne.surf.odyssey.runtime.paper.hub.lobby.doublejump.DoubleJumpListener
import dev.slne.surf.odyssey.runtime.paper.progression.BlockPos
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionPlacements
import dev.slne.surf.odyssey.runtime.paper.progression.SpawnPoint
import net.kyori.adventure.text.Component
import org.bukkit.plugin.java.JavaPlugin

/** Plugin of the lobby hub server: the network's entry point with the double jump. */
class LobbyHubPlugin : HubPlugin() {
    /** Shown to players as "Lobby". */
    override val worldDisplayName = Component.text("Lobby")

    /** The progression content of the network. */
    override val content = OdysseyContent

    /** Placeholder coordinate until the lobby map's real spawn point is known. */
    override val spawnPoint = WorldPosition(0.0, 64.0, 0.0, 0f, 0f)

    /** Declares the portals to North and South. */
    override fun portals() = lobbyPortals()

    /** Places Brina, the cook and the workbench. */
    override fun ProgressionPlacements.placements() = lobbyPlacements()

    /** Registers the lobby's listeners. */
    override suspend fun onHubEnabled() {
        DoubleJumpListener.register()
    }
}

/** The portals of the lobby: to North and to South. Coordinates are placeholders until the map is in place. */
fun lobbyPortals() = listOf(
    Portal("to_north", shape { cuboid(10, 64, -2, 10, 67, 2) }, "odyssey-hub-north", WorldPosition(8.5, 64.0, 0.5, 90f, 0f)),
    Portal("to_south", shape { cuboid(-10, 64, -2, -10, 67, 2) }, "odyssey-hub-south", WorldPosition(-7.5, 64.0, 0.5, -90f, 0f)),
)

/**
 * Places the progression of the lobby on the spawn platform: Brina and Koch Anselm stand north of the spawn, facing
 * it, and the workbench is the smooth stone platform block at 0 63 5 in front of the spawn, which opens the
 * workbench when right-clicked.
 */
fun ProgressionPlacements.lobbyPlacements() {
    npc(Npcs.BRINA, SpawnPoint(3.5, 64.0, -4.5, 45f, 0f))
    npc(Npcs.KOCH, SpawnPoint(-3.5, 64.0, -4.5, -45f, 0f))
    station(Stations.WORKBENCH, BlockPos(0, 63, 5))
}

/** The running lobby plugin instance. */
val plugin get() = JavaPlugin.getPlugin(LobbyHubPlugin::class.java)
