package dev.slne.surf.odyssey.runtime.paper.hub.common

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperMain
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperServerType
import dev.slne.surf.odyssey.runtime.paper.hub.common.editor.EditorPreviewTask
import dev.slne.surf.odyssey.runtime.paper.hub.common.editor.RegionWandListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.editor.regionEditorCommand
import dev.slne.surf.odyssey.runtime.paper.hub.common.fishing.FishingListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.gate.GateEnforcer
import dev.slne.surf.odyssey.runtime.paper.hub.common.gate.GateListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.mobs.MobKillListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.mobs.MobRegionLoot
import dev.slne.surf.odyssey.runtime.paper.hub.common.mobs.MobSpawnRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.placeholder.HubPlaceholders
import dev.slne.surf.odyssey.runtime.paper.hub.common.progression.HubProgression
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.HubProtectionListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.ProtectionSettings
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.ProtectionSettingsBuilder
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.WorldChangeListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect.ReconnectService
import dev.slne.surf.odyssey.runtime.paper.hub.common.redis.HubRedis
import dev.slne.surf.odyssey.runtime.paper.hub.common.regeneration.RegenerationTicker
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionIndex
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionTicker
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.ArrivalListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.Portal
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.PortalListener
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionPlacements
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentPack
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import dev.slne.surf.odyssey.runtime.paper.progression.vanilla.VanillaLockdown
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

    /** Changes allowed everywhere in this hub world. Available once the plugin is enabled. */
    lateinit var protectionSettings: ProtectionSettings
        private set

    /** The world the regions belong to: the server's main world. */
    val hubWorld: World get() = server.worlds.first()

    /** Portals of this hub world. Available once the plugin is enabled. */
    lateinit var portalList: List<Portal>
        private set

    /** The progression content this hub world installs. */
    protected abstract val content: ContentPack

    /**
     * The requirement a player must meet to stay in this hub world, or `null` if it is open to everyone. A player
     * who does not meet it is sent to the lobby once their progression has loaded after joining.
     */
    open val worldGate: Requirement? = null

    /** Declares the regions of this hub world. */
    protected open fun RegionRegistry.regions() {}

    /** Declares where the NPCs and stations of this hub world stand. */
    protected open fun ProgressionPlacements.placements() {}

    /** Declares the portals of this hub world. */
    protected open fun portals(): List<Portal> = emptyList()

    /** Declares the changes allowed everywhere in this hub world; by default every change is blocked. */
    protected open fun ProtectionSettingsBuilder.settings() {}

    /** Registers the inventory views of the progression engine. */
    final override suspend fun onLoaded() {
        ProgressionModule.registerViews()
    }

    /**
     * Builds the region index and the world's protection settings, collects the portals, connects the
     * arrival and reconnect caches to Redis, starts the reconnect service, turns off saving for every world,
     * removes vanilla recipes and loot except the code loot of mob regions, installs the progression engine
     * with [content] and the [placements], registers the gate, protection, world change, fishing, mob kill,
     * portal, arrival and region wand listeners and the `/hubregion` command, starts the editor preview, the
     * region ticker and the gate enforcer, registers the placeholder tags, then calls [onHubEnabled].
     *
     * @throws IllegalStateException if two regions share an id or a block; nothing else is set up then
     */
    final override suspend fun onEnabled() {
        regionIndex = RegionIndex(RegionRegistry().apply { regions() }.build())
        protectionSettings = ProtectionSettingsBuilder().apply { settings() }.build()
        portalList = portals()
        HubRedis.connect(name)
        ReconnectService.start(this)
        server.worlds.forEach { it.isAutoSave = false }
        VanillaLockdown.install(this, MobRegionLoot)
        HubProgression.install(this, content, ProgressionPlacements().apply { placements() })
        GateListener.register()
        HubProtectionListener.register()
        WorldChangeListener.register()
        FishingListener.register()
        MobKillListener.register()
        PortalListener.register()
        ArrivalListener.register()
        regionEditorCommand()
        RegionWandListener.register()
        EditorPreviewTask.start(this)
        RegionTicker.start(this)
        GateEnforcer.start(this)
        HubPlaceholders.register(this)
        onHubEnabled()
    }

    /**
     * Calls [onHubDisabled], then despawns the progression NPCs and sends the buffered progress, restores
     * every block waiting to regenerate, unregisters the placeholder tags, stops ticking and enforcing gates,
     * removes the mobs of every mob spawn region, stops the reconnect service and disconnects from Redis.
     * Each step runs even if an earlier one throws; the first failure is thrown once all steps have run.
     */
    final override suspend fun onDisabled() {
        val steps = ShutdownSteps()
        steps.run { onHubDisabled() }
        steps.run { HubProgression.shutdown() }
        steps.run { RegenerationTicker.restoreAll(hubWorld) }
        steps.run { HubPlaceholders.unregister() }
        steps.run { RegionTicker.stop() }
        steps.run { GateEnforcer.stop() }
        steps.run {
            if (this::regionIndex.isInitialized) regionIndex.regionsOfType<MobSpawnRegion>().forEach { it.removeMobs() }
        }
        steps.run { ReconnectService.stop(this) }
        steps.run { HubRedis.disconnect() }
        steps.rethrow()
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
