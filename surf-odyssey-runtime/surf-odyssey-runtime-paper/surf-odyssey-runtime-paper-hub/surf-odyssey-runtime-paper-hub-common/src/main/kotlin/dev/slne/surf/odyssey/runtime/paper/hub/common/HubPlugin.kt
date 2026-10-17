package dev.slne.surf.odyssey.runtime.paper.hub.common

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperMain
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperServerType
import dev.slne.surf.odyssey.runtime.paper.hub.common.editor.EditorPreviewTask
import dev.slne.surf.odyssey.runtime.paper.hub.common.editor.RegionWandListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.editor.regionEditorCommand
import dev.slne.surf.odyssey.runtime.paper.hub.common.fishing.FishingListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.placeholder.HubPlaceholders
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.HubProtectionListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.WorldChangeListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.regeneration.RegenerationTicker
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionIndex
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionTicker
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.ArrivalCache
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.ArrivalListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.Portal
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.PortalListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition
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

    /**
     * Where players spawn when they do not arrive through a portal, or `null` for the world's spawn.
     */
    open val spawnPoint: WorldPosition? = null

    /** The world the regions belong to: the server's main world. */
    val hubWorld: World get() = server.worlds.first()

    /** Portals of this hub world. Available once the plugin is enabled. */
    lateinit var portalList: List<Portal>
        private set

    /** Declares the regions of this hub world. */
    protected open fun RegionRegistry.regions() {}

    /** Declares the portals of this hub world. */
    protected open fun portals(): List<Portal> = emptyList()

    /**
     * Builds the region index, collects the portals, connects the arrival cache, turns off saving for every
     * world, registers protection, world change handling, fishing, portals, arrivals and the region editor
     * with its wand, starts ticking and the editor preview, registers the placeholder tags, then calls
     * [onHubEnabled].
     *
     * @throws IllegalStateException if two regions share an id or a block; nothing else is set up then
     */
    final override suspend fun onEnabled() {
        regionIndex = RegionIndex(RegionRegistry().apply { regions() }.build())
        portalList = portals()
        ArrivalCache.connect(name)
        server.worlds.forEach { it.isAutoSave = false }
        HubProtectionListener.register()
        WorldChangeListener.register()
        FishingListener.register()
        PortalListener.register()
        ArrivalListener.register()
        regionEditorCommand()
        RegionWandListener.register()
        EditorPreviewTask.start(this)
        RegionTicker.start(this)
        HubPlaceholders.register(this)
        onHubEnabled()
    }

    /**
     * Calls [onHubDisabled], restores every block waiting to regenerate, unregisters the placeholder tags, stops
     * ticking, then disconnects the arrival cache. The arrival cache is disconnected even if an earlier step fails.
     */
    final override suspend fun onDisabled() {
        try {
            onHubDisabled()
            RegenerationTicker.restoreAll(hubWorld)
            HubPlaceholders.unregister()
            RegionTicker.stop()
        } finally {
            ArrivalCache.disconnect()
        }
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
