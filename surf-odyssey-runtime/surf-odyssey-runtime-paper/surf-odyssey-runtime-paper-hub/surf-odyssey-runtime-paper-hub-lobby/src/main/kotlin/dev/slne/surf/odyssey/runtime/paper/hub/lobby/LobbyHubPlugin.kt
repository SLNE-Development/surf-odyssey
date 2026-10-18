package dev.slne.surf.odyssey.runtime.paper.hub.lobby

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.shape
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.Portal
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition
import dev.slne.surf.odyssey.runtime.paper.hub.lobby.doublejump.DoubleJumpListener
import net.kyori.adventure.text.Component
import org.bukkit.plugin.java.JavaPlugin

/** Plugin of the lobby hub server: the network's entry point with the double jump. */
class LobbyHubPlugin : HubPlugin() {
    /** Shown to players as "Lobby". */
    override val worldDisplayName = Component.text("Lobby")

    /** Placeholder coordinate until the lobby map's real spawn point is known. */
    override val spawnPoint = WorldPosition(0.0, 64.0, 0.0, 0f, 0f)

    /** Declares the portals to North and South. Coordinates are placeholders until the map is in place. */
    override fun portals() = listOf(
        Portal("to_north", shape { cuboid(10, 64, -2, 10, 67, 2) }, "odyssey-hub-north", WorldPosition(8.5, 64.0, 0.5, 90f, 0f)),
        Portal("to_south", shape { cuboid(-10, 64, -2, -10, 67, 2) }, "odyssey-hub-south", WorldPosition(-7.5, 64.0, 0.5, -90f, 0f)),
    )

    /** Registers the lobby's listeners. */
    override suspend fun onHubEnabled() {
        DoubleJumpListener.register()
    }
}

/** The running lobby plugin instance. */
val plugin get() = JavaPlugin.getPlugin(LobbyHubPlugin::class.java)
