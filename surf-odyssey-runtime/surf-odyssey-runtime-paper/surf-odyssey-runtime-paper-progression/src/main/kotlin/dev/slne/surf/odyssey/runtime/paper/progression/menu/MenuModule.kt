package dev.slne.surf.odyssey.runtime.paper.progression.menu

import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionRuntime
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

/** Installs the player menus of the progression engine: the Odyssey menu and the dialogs and views it opens. */
object MenuModule {
    /**
     * Registers the listeners of the menus, gives every online player the [OdysseyBook] and, if asked, registers the
     * placeholder tags as an own `odyssey` expansion.
     *
     * @param plugin the plugin that owns the listeners
     * @param runtime the installed engine
     * @param standalonePlaceholders whether to register [ProgressionPlaceholders] as an own expansion, for servers
     *        whose plugin does not add the tags to its own `odyssey` expansion
     */
    fun install(plugin: Plugin, runtime: ProgressionRuntime, standalonePlaceholders: Boolean = false) {
        Bukkit.getPluginManager().registerEvents(MenuClickListener(plugin), plugin)
        Bukkit.getPluginManager().registerEvents(OdysseyBookListener(plugin), plugin)
        Bukkit.getOnlinePlayers().forEach { player -> player.scheduler.run(plugin, { OdysseyBook.ensure(player) }, null) }
        if (standalonePlaceholders) ProgressionPlaceholders.registerStandalone()
    }
}
