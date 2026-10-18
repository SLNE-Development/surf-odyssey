package dev.slne.surf.odyssey.runtime.paper.home

import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperMain
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperServerType
import dev.slne.surf.odyssey.runtime.paper.content.OdysseyContent
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMode
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.vanilla.MobLootProvider
import dev.slne.surf.odyssey.runtime.paper.progression.vanilla.VanillaLockdown
import org.bukkit.plugin.java.JavaPlugin

/**
 * Plugin of the home server. Players see their quests, titles and pickup box here through the progression engine in
 * [ProgressionMode.VIEW_ONLY]; NPCs, stations and tools stay in the hub worlds.
 */
class HomePaperMain : OdysseyPaperMain() {
    /** Registers the inventory views of the progression engine. */
    override suspend fun onLoaded() {
        ProgressionModule.registerViews()
    }

    /**
     * Removes vanilla recipes and mob loot, then installs the progression engine in [ProgressionMode.VIEW_ONLY] with
     * [OdysseyContent] and its placeholder tags as an own `odyssey` expansion.
     */
    override suspend fun onEnabled() {
        VanillaLockdown.install(this, MobLootProvider.NONE)
        ProgressionModule.install(
            this,
            OdysseyContent,
            ProgressionMode.VIEW_ONLY,
            serverName = { SERVER_NAME },
            standalonePlaceholders = true,
        )
    }

    /** Sends the buffered progress of the progression engine; does nothing if it was not installed. */
    override suspend fun onDisabled() {
        Progression.runtimeOrNull?.shutdown()
    }

    /** Always [OdysseyPaperServerType.HOME]. */
    override val serverType = OdysseyPaperServerType.HOME

    /** Holds the server name given to the progression engine. */
    private companion object {
        /** Name of this server as the progression engine reports it. */
        const val SERVER_NAME: String = "home"
    }
}

/** The running home plugin instance. */
val plugin get() = JavaPlugin.getPlugin(HomePaperMain::class.java)
