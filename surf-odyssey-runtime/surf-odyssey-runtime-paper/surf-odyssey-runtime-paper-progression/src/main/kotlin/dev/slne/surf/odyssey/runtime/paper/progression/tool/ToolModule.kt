package dev.slne.surf.odyssey.runtime.paper.progression.tool

import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionRuntime
import org.bukkit.plugin.Plugin

/** Installs the Paper side of progression tools on servers where tools level. */
object ToolModule {
    /**
     * Registers the listeners that keep the owner chains of tool items current.
     *
     * @param plugin the plugin that owns the listeners
     * @param runtime the installed progression engine
     */
    fun install(plugin: Plugin, runtime: ProgressionRuntime) {
        plugin.server.pluginManager.registerEvents(ToolProgressListener(), plugin)
    }
}
