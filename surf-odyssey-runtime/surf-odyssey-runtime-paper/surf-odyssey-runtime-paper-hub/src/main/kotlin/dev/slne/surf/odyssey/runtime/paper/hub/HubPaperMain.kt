package dev.slne.surf.odyssey.runtime.paper.hub

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperMain
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperServerType
import dev.slne.surf.odyssey.runtime.paper.hub.skill.fishing.FishingListener
import dev.slne.surf.odyssey.runtime.paper.hub.skill.mining.MiningListener
import dev.slne.surf.odyssey.runtime.paper.hub.skill.mobhunting.MobHuntingListener
import org.bukkit.plugin.java.JavaPlugin

class HubPaperMain : OdysseyPaperMain() {
    override suspend fun onLoaded() {

    }

    override suspend fun onEnabled() {
        FishingListener.register()
        MiningListener.register()
        MobHuntingListener.register()
    }

    override suspend fun onDisabled() {

    }

    override val serverType = OdysseyPaperServerType.HUB
}

val plugin get() = JavaPlugin.getPlugin(HubPaperMain::class.java)
