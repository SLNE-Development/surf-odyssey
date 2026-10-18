package dev.slne.surf.odyssey.runtime.paper.home

import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperMain
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperServerType
import dev.slne.surf.odyssey.runtime.paper.progression.vanilla.MobLootProvider
import dev.slne.surf.odyssey.runtime.paper.progression.vanilla.VanillaLockdown
import org.bukkit.plugin.java.JavaPlugin

class HomePaperMain : OdysseyPaperMain() {
    override suspend fun onLoaded() {

    }

    override suspend fun onEnabled() {
        VanillaLockdown.install(this, MobLootProvider.NONE)
    }

    override suspend fun onDisabled() {

    }

    override val serverType = OdysseyPaperServerType.HOME
}

val plugin get() = JavaPlugin.getPlugin(HomePaperMain::class.java)