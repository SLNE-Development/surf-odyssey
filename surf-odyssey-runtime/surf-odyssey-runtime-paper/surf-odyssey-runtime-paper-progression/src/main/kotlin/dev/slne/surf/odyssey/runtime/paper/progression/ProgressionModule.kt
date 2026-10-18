package dev.slne.surf.odyssey.runtime.paper.progression

import dev.slne.surf.api.paper.inventory.framework.register
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentPack
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import dev.slne.surf.odyssey.runtime.paper.progression.delivery.pickupBoxView
import dev.slne.surf.odyssey.runtime.paper.progression.operation.LoggingCurrencyGateway
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationRunner
import dev.slne.surf.odyssey.runtime.paper.progression.operation.RewardPlanner
import dev.slne.surf.odyssey.runtime.paper.progression.operation.ToolRewardFactory
import dev.slne.surf.odyssey.runtime.paper.progression.state.ProgressionStore
import dev.slne.surf.odyssey.runtime.paper.progression.station.recipeBrowserView
import dev.slne.surf.odyssey.runtime.paper.progression.station.stationView
import dev.slne.surf.odyssey.runtime.paper.progression.trade.tradeView
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ToolItems
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ToolModule
import org.bukkit.plugin.Plugin
import java.time.Clock

/**
 * Installs the progression engine, which holds the definition DSL, the registry and the Paper runtime of quests,
 * conversations, stations, recipes, traders, tools and titles.
 */
object ProgressionModule {
    /** The plugin the engine was installed by. */
    internal lateinit var plugin: Plugin
        private set

    /** Registers every inventory view of the engine; must run during plugin load. */
    fun registerViews() {
        pickupBoxView.register()
        stationView.register()
        recipeBrowserView.register()
        tradeView.register()
    }

    /**
     * Builds the registry from [pack], starts loading and saving the players' progression, sets
     * [Progression.runtime] and lets tool rewards create tool items. In [ProgressionMode.FULL] it also installs the
     * tool listeners with [ToolModule.install].
     *
     * @param plugin the plugin that owns the listeners, coroutines and log
     * @param pack the content of this server
     * @param mode which parts of the engine run
     * @param clock the clock every time of the engine comes from
     * @return the installed engine
     * @throws dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentValidationException if
     *         the content is invalid
     */
    fun install(
        plugin: Plugin,
        pack: ContentPack,
        mode: ProgressionMode,
        clock: Clock = Clock.systemUTC(),
    ): ProgressionRuntime {
        val registry = ProgressionRegistry.build(pack)
        this.plugin = plugin
        val store = ProgressionStore(plugin, clock, onLoaded = { player, progression ->
            val waiting = progression.state.deliveries.size
            if (waiting > 0) ProgressionMessages.deliveriesWaiting(player, waiting)
        })
        val operations = OperationRunner(plugin, store, LoggingCurrencyGateway(plugin.logger))
        val runtime = ProgressionRuntime(registry, mode, store, operations, clock)
        Progression.runtime = runtime
        RewardPlanner.toolRewards = ToolRewardFactory(ToolItems::create)
        store.start()
        if (mode == ProgressionMode.FULL) ToolModule.install(plugin, runtime)
        return runtime
    }
}
