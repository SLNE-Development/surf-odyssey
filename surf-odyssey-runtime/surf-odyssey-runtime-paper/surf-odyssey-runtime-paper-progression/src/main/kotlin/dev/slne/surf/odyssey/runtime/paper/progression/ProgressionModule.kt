package dev.slne.surf.odyssey.runtime.paper.progression

import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentPack
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import dev.slne.surf.odyssey.runtime.paper.progression.operation.LoggingCurrencyGateway
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationRunner
import dev.slne.surf.odyssey.runtime.paper.progression.state.ProgressionStore
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

    /**
     * Builds the registry from [pack], starts loading and saving the players' progression and sets
     * [Progression.runtime].
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
        store.start()
        return runtime
    }
}
