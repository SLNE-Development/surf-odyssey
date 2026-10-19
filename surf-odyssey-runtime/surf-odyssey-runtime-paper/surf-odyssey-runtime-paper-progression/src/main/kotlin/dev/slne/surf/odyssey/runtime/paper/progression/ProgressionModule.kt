package dev.slne.surf.odyssey.runtime.paper.progression

import dev.slne.surf.api.paper.inventory.framework.register
import dev.slne.surf.odyssey.runtime.paper.common.scoreboard.ScoreboardProviders
import dev.slne.surf.odyssey.runtime.paper.progression.admin.progressionAdminCommand
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentPack
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import dev.slne.surf.odyssey.runtime.paper.progression.delivery.pickupBoxView
import dev.slne.surf.odyssey.runtime.paper.progression.dialog.Conversations
import dev.slne.surf.odyssey.runtime.paper.progression.npc.ProgressionNpcs
import dev.slne.surf.odyssey.runtime.paper.progression.menu.MenuModule
import dev.slne.surf.odyssey.runtime.paper.progression.operation.LoggingCurrencyGateway
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationRunner
import dev.slne.surf.odyssey.runtime.paper.progression.operation.RewardPlanner
import dev.slne.surf.odyssey.runtime.paper.progression.scoreboard.ProgressionScoreboardProvider
import dev.slne.surf.odyssey.runtime.paper.progression.operation.ToolRewardFactory
import dev.slne.surf.odyssey.runtime.paper.progression.state.ProgressionStore
import dev.slne.surf.odyssey.runtime.paper.progression.dialog.ConversationHooks
import dev.slne.surf.odyssey.runtime.paper.progression.station.StationBlocks
import dev.slne.surf.odyssey.runtime.paper.progression.station.StationView
import dev.slne.surf.odyssey.runtime.paper.progression.station.recipeBrowserView
import dev.slne.surf.odyssey.runtime.paper.progression.trade.TradeView
import dev.slne.surf.odyssey.runtime.paper.progression.station.stationView
import dev.slne.surf.odyssey.runtime.paper.progression.trade.tradeView
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ToolItems
import dev.slne.surf.odyssey.runtime.paper.progression.tool.accessoryView
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ToolModule
import org.bukkit.Bukkit
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
        accessoryView.register()
    }

    /**
     * Builds the registry from [pack], starts loading and saving the players' progression, sets
     * [Progression.runtime] and lets tool rewards create tool items. In [ProgressionMode.FULL] it also installs the
     * tool listeners with [ToolModule.install], makes the placed station blocks of the main world open their
     * stations, connects the "Handeln" and "Herstellen" answers of conversations to the trade and station menus and
     * spawns the placed NPCs. In every mode it installs the player menus and registers `/odysseyadmin`, which
     * offers only `quest info` in [ProgressionMode.VIEW_ONLY].
     *
     * @param plugin the plugin that owns the listeners, coroutines and log
     * @param pack the content of this server
     * @param mode which parts of the engine run
     * @param serverName returns the name of this server, as sent with every tool sighting
     * @param placements where the NPCs and stations of this server stand, in the main world; used only in
     *        [ProgressionMode.FULL]
     * @param clock the clock every time of the engine comes from
     * @param standalonePlaceholders whether the engine registers its placeholder tags as an own `odyssey` expansion
     * @return the installed engine
     * @throws dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentValidationException if
     *         the content is invalid
     */
    fun install(
        plugin: Plugin,
        pack: ContentPack,
        mode: ProgressionMode,
        serverName: () -> String,
        placements: ProgressionPlacements = ProgressionPlacements(),
        clock: Clock = Clock.systemUTC(),
        standalonePlaceholders: Boolean = false,
    ): ProgressionRuntime {
        val registry = ProgressionRegistry.build(pack)
        this.plugin = plugin
        val store = ProgressionStore(plugin, clock, onLoaded = { player, progression ->
            val waiting = progression.state.deliveries.size
            if (waiting > 0) ProgressionMessages.deliveriesWaiting(player, waiting)
            ProgressionNpcs.refresh(player)
        })
        val operations = OperationRunner(plugin, store, LoggingCurrencyGateway(plugin.logger))
        val runtime = ProgressionRuntime(registry, mode, store, operations, clock)
        Progression.runtime = runtime
        ScoreboardProviders.register(ProgressionScoreboardProvider)
        RewardPlanner.toolRewards = ToolRewardFactory(ToolItems::create)
        store.start()
        progressionAdminCommand(mode)
        if (mode == ProgressionMode.FULL) {
            ToolModule.install(plugin, runtime, serverName)
            StationBlocks.install(plugin, Bukkit.getWorlds().first(), placements.stations)
            ConversationHooks.openTrade = { player, npc -> TradeView.open(player, npc) }
            ConversationHooks.openStation = { player, npc -> npc.station?.let { StationView.open(player, it) } }
            Conversations.register(plugin)
            ProgressionNpcs.install(plugin, runtime, placements.npcs, onInteract = Conversations::open)
        }
        MenuModule.install(plugin, runtime, standalonePlaceholders)
        return runtime
    }
}
