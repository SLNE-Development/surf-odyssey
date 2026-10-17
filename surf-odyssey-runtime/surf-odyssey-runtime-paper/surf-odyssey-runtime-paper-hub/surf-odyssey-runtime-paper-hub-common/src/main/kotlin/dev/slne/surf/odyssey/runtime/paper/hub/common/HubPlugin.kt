package dev.slne.surf.odyssey.runtime.paper.hub.common

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperMain
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperServerType
import dev.slne.surf.odyssey.runtime.paper.hub.common.fishing.FishingListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.HubProtectionListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.WorldChangeListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.regeneration.RegenerationTicker
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionIndex
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionTicker
import net.kyori.adventure.text.Component
import org.bukkit.World

/**
 * Base plugin of a hub world server.
 *
 * A world plugin names its world and declares its regions, and gets protection and regeneration
 * from this class.
 *
 * The map stays intact because the server copies the world fresh on every start. In addition,
 * automatic saving is turned off for every world and, when the plugin is disabled, every block
 * still waiting to regenerate is restored before ticking stops.
 *
 * If two declared regions share an id or a block, enabling fails with an error naming both
 * regions and the server disables the plugin.
 */
abstract class HubPlugin : OdysseyPaperMain() {
    /** Always [OdysseyPaperServerType.HUB]. */
    final override val serverType = OdysseyPaperServerType.HUB

    /** Name of this hub world shown to players, e.g. "Norden". */
    abstract val worldDisplayName: Component

    /** Regions of this hub world. Available once the plugin is enabled. */
    lateinit var regionIndex: RegionIndex
        private set

    /** The world the regions belong to: the server's main world. */
    val hubWorld: World get() = server.worlds.first()

    /** Declares the regions of this hub world. */
    protected open fun RegionRegistry.regions() {}

    /**
     * Builds the region index, turns off saving for every world, registers protection, world change
     * handling and fishing, starts ticking,
     * then calls [onHubEnabled].
     *
     * @throws IllegalStateException if two regions share an id or a block; nothing else is set up then
     */
    final override suspend fun onEnabled() {
        regionIndex = RegionIndex(RegionRegistry().apply { regions() }.build())
        server.worlds.forEach { it.isAutoSave = false }
        HubProtectionListener.register()
        WorldChangeListener.register()
        FishingListener.register()
        RegionTicker.start(this)
        onHubEnabled()
    }

    /** Calls [onHubDisabled], restores every block waiting to regenerate, then stops ticking. */
    final override suspend fun onDisabled() {
        onHubDisabled()
        RegenerationTicker.restoreAll(hubWorld)
        RegionTicker.stop()
    }

    /** Called after the hub systems are running. */
    protected open suspend fun onHubEnabled() {}

    /** Called before the hub systems stop. */
    protected open suspend fun onHubDisabled() {}

    companion object {
        /** The running hub plugin. */
        val instance: HubPlugin get() = OdysseyPaperMain.INSTANCE as HubPlugin
    }
}
