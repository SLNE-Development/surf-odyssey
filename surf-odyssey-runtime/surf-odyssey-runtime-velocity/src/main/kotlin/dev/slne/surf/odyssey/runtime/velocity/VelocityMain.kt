package dev.slne.surf.odyssey.runtime.velocity

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path

class VelocityMain @Inject constructor(
    val server: ProxyServer,
    val container: PluginContainer,
    suspendingPluginContainer: SuspendingPluginContainer,
    @DataDirectory val dataPath: Path
) {
    init {
        suspendingPluginContainer.initialize(this)
    }

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {

    }

    companion object {
        lateinit var INSTANCE: VelocityMain
            private set
    }
}

val plugin get() = VelocityMain.INSTANCE
val server get() = plugin.server
val container get() = plugin.container