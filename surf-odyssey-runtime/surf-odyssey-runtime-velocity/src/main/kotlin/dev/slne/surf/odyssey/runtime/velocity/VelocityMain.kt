package dev.slne.surf.odyssey.runtime.velocity

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.slne.surf.odyssey.runtime.velocity.lobby.InitialLobbyListener
import dev.slne.surf.odyssey.runtime.velocity.lobby.ReconnectCleanupListener
import dev.slne.surf.odyssey.runtime.velocity.lobby.ReconnectLookup
import java.nio.file.Path

class VelocityMain @Inject constructor(
    val server: ProxyServer,
    val container: PluginContainer,
    suspendingPluginContainer: SuspendingPluginContainer,
    @DataDirectory val dataPath: Path
) {
    init {
        INSTANCE = this
        suspendingPluginContainer.initialize(this)
    }

    /** Connects the reconnect lookup to Redis and registers the initial server choice and the reconnect cleanup. */
    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        ReconnectLookup.connect(container.description.id)
        server.eventManager.register(this, InitialLobbyListener)
        server.eventManager.register(this, ReconnectCleanupListener)
    }

    /** Disconnects the reconnect lookup from Redis. */
    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        ReconnectLookup.disconnect()
    }

    companion object {
        lateinit var INSTANCE: VelocityMain
            private set
    }
}

val plugin get() = VelocityMain.INSTANCE
val server get() = plugin.server
val container get() = plugin.container