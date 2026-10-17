package dev.slne.surf.odyssey.runtime.paper.hub

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperMain
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperServerType
import dev.slne.surf.odyssey.runtime.paper.hub.doublejump.DoubleJumpListener
import org.bukkit.plugin.java.JavaPlugin

class HubPaperMain : OdysseyPaperMain() {
    override suspend fun onLoaded() {

    }

    override suspend fun onEnabled() {
        DoubleJumpListener.register()
    }

    override suspend fun onDisabled() {
    }

    override val serverType = OdysseyPaperServerType.HUB
}

val plugin get() = JavaPlugin.getPlugin(HubPaperMain::class.java)
