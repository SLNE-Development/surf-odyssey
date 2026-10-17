package dev.slne.surf.odyssey.runtime.paper.hub

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperMain
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperServerType
import dev.slne.surf.odyssey.runtime.paper.hub.doublejump.DoubleJumpListener
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorld
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorldBootstrap
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.command.skillWorldCommand
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.hud.RegionHud
import org.bukkit.plugin.java.JavaPlugin

class HubPaperMain : OdysseyPaperMain() {
    override suspend fun onLoaded() {

    }

    override suspend fun onEnabled() {
        DoubleJumpListener.register()

        skillWorldCommand()

        RegionHud.register()
        RegionHud.startForOnlinePlayers()

        if (SkillWorldBootstrap.setup()) plugin.launch { SkillWorld.load() }
    }

    override suspend fun onDisabled() {
        SkillWorld.shutdown()
    }

    override val serverType = OdysseyPaperServerType.HUB
}

val plugin get() = JavaPlugin.getPlugin(HubPaperMain::class.java)
