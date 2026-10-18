package dev.slne.surf.odyssey.runtime.paper.progression.menu

import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionRuntime
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

/** Installs the player menus of the progression engine: the Odyssey menu and the dialogs and views it opens. */
object MenuModule {
    /**
     * Registers the listeners of the menus.
     *
     * @param plugin the plugin that owns the listeners
     * @param runtime the installed engine
     */
    fun install(plugin: Plugin, runtime: ProgressionRuntime) {
        Bukkit.getPluginManager().registerEvents(MenuClickListener(plugin), plugin)
    }
}
