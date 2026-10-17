package dev.slne.surf.odyssey.runtime.paper.hub.lobby

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.lobby.doublejump.DoubleJumpListener
import net.kyori.adventure.text.Component
import org.bukkit.plugin.java.JavaPlugin

/** Plugin of the lobby hub server: the network's entry point with the double jump. */
class LobbyHubPlugin : HubPlugin() {
    /** Shown to players as "Lobby". */
    override val worldDisplayName = Component.text("Lobby")

    /** Registers the lobby's listeners. */
    override suspend fun onHubEnabled() {
        DoubleJumpListener.register()
    }
}

/** The running lobby plugin instance. */
val plugin get() = JavaPlugin.getPlugin(LobbyHubPlugin::class.java)
