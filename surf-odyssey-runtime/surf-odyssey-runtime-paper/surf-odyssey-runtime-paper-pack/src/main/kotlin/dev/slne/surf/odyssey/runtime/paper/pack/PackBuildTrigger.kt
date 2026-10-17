package dev.slne.surf.odyssey.runtime.paper.pack

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent

/**
 * Starts the CraftEngine pack workflow, which builds the resource pack and uploads it, once the server has
 * finished starting.
 *
 * CraftEngine runs its workflows only on a reload, so without this trigger a fresh pack server would never
 * upload a pack.
 *
 * @property dispatchConsoleCommand runs the given command line as the console, without a leading slash
 */
class PackBuildTrigger(private val dispatchConsoleCommand: (String) -> Unit) : Listener {
    /** Passes the load type of [event] to [onLoad]. */
    @EventHandler
    fun onServerLoad(event: ServerLoadEvent) = onLoad(event.type)

    /** Dispatches [RELOAD_COMMAND] when the server has finished its startup; ignores reloads of the server. */
    fun onLoad(type: ServerLoadEvent.LoadType) {
        if (type == ServerLoadEvent.LoadType.STARTUP) dispatchConsoleCommand(RELOAD_COMMAND)
    }

    private companion object {
        /** Reloads CraftEngine's configuration and pack, which runs the workflows triggered by a pack reload. */
        const val RELOAD_COMMAND = "ce reload all"
    }
}
