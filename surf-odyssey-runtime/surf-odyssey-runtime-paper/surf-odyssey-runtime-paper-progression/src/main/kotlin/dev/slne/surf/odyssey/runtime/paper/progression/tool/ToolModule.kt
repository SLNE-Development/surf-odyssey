package dev.slne.surf.odyssey.runtime.paper.progression.tool

import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionRuntime
import org.bukkit.plugin.Plugin

/** Installs the Paper side of progression tools on servers where tools level. */
object ToolModule {
    /**
     * Registers the listeners that keep the owner chains of tool items current, open the accessory view and write
     * buffered tool usage, starts the loop that writes buffered usage after its interval, and starts reporting tool
     * sightings: every minute for all online players and immediately when a player takes a tool over.
     *
     * @param plugin the plugin that owns the listeners
     * @param runtime the installed progression engine
     * @param serverName returns the name of this server, as sent with every tool sighting
     */
    fun install(plugin: Plugin, runtime: ProgressionRuntime, serverName: () -> String) {
        val sightings = ToolSightings(plugin, serverName)
        plugin.server.pluginManager.registerEvents(ToolProgressListener(sightings::reportNow), plugin)
        plugin.server.pluginManager.registerEvents(ToolWriteListener(), plugin)
        ToolWriteListener.startWriteLoop(plugin)
        sightings.start()
    }
}
