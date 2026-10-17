package dev.slne.surf.odyssey.runtime.paper.hub.lobby

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperMain
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperServerType
import dev.slne.surf.odyssey.runtime.paper.hub.lobby.doublejump.DoubleJumpListener
import org.bukkit.plugin.java.JavaPlugin

/** Plugin of the lobby hub server: the network's entry point with the double jump. */
class LobbyHubPlugin : OdysseyPaperMain() {
    override val serverType = OdysseyPaperServerType.HUB

    /** Registers the lobby's listeners. */
    override suspend fun onEnabled() {
        DoubleJumpListener.register()
    }
}

/** The running lobby plugin instance. */
val plugin get() = JavaPlugin.getPlugin(LobbyHubPlugin::class.java)
