package dev.slne.surf.odyssey.runtime.paper.progression.npc

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionRuntime
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import kotlin.time.Duration.Companion.milliseconds

/**
 * Spawns the placed NPCs of this server and keeps their visibility per player up to date.
 *
 * The NPCs are spawned in the main world on the first tick after [install] and removed by [despawnAll]. A player's
 * visibility is recomputed when they join, when their progression is loaded and after each of their successful
 * operations. Interactions are passed on at most once per [INTERACT_COOLDOWN] per player.
 */
object ProgressionNpcs {
    /** The time after an interaction during which further interactions of the same player are ignored. */
    val INTERACT_COOLDOWN = 500.milliseconds

    /** The name of the plugin that provides the NPCs. */
    const val NPC_PLUGIN = "surf-npc-paper"

    /** The plugin that installed the NPCs, or `null` before [install]. */
    @Volatile
    private var plugin: Plugin? = null

    /** The backend that spawns the NPCs, or `null` before [install]. */
    @Volatile
    private var backend: NpcBackend? = null

    /** The visibility of the spawned NPCs, or `null` while none are spawned. */
    @Volatile
    private var visibility: NpcVisibility? = null

    /**
     * Spawns the NPCs of [placements] in the main world on the next tick and starts tracking their visibility.
     * Does nothing but log a warning if the NPC plugin is not installed.
     *
     * @param plugin the plugin that owns the listeners and tasks
     * @param runtime the installed engine
     * @param placements where the NPCs stand
     * @param onInteract called on the player's thread with the player, the NPC and where it stands when a player
     *        interacts with an NPC
     */
    internal fun install(
        plugin: Plugin,
        runtime: ProgressionRuntime,
        placements: List<NpcPlacement>,
        onInteract: (Player, NpcDefinition, Location) -> Unit,
    ) {
        if (Bukkit.getPluginManager().getPlugin(NPC_PLUGIN) == null) {
            plugin.logger.warning("$NPC_PLUGIN is not installed, so no progression npcs are spawned")
            return
        }
        this.plugin = plugin
        val cooldown = InteractCooldown(INTERACT_COOLDOWN)
        val backend = SurfNpcBackend { player, npc ->
            if (cooldown.tryAcquire(player.uniqueId)) onInteract(player, npc.definition, npc.location)
        }
        Bukkit.getPluginManager().registerEvents(backend, plugin)
        this.backend = backend
        Bukkit.getPluginManager().registerEvents(Lifecycle(cooldown), plugin)
        runtime.operations.onSuccess(::refresh)
        Bukkit.getGlobalRegionScheduler().run(plugin) {
            val (known, unknown) = placements.partition { runtime.registry.npc(it.npc.id) === it.npc }
            unknown.forEach {
                plugin.logger.warning("The npc ${it.npc.id} is placed but not part of the content, so it is not spawned")
            }
            val world = Bukkit.getWorlds().first()
            val spawned = known.map { placement ->
                val at = placement.position
                backend.spawn(placement.npc, Location(world, at.x, at.y, at.z, at.yaw, at.pitch))
            }
            visibility = NpcVisibility(backend, spawned, runtime.store)
            Bukkit.getOnlinePlayers().forEach(::refresh)
        }
    }

    /**
     * Recomputes which NPCs [player] sees, on the player's thread. Does nothing while no NPCs are spawned.
     *
     * @param player the viewer
     */
    fun refresh(player: Player) {
        val plugin = plugin ?: return
        val visibility = visibility ?: return
        plugin.launch(plugin.entityDispatcher(player)) {
            if (player.isOnline) visibility.refresh(player)
        }
    }

    /** Removes every spawned NPC; players see none until the NPCs are installed again. */
    fun despawnAll() {
        visibility = null
        backend?.despawnAll()
    }

    /**
     * Updates the NPCs when players join or leave.
     *
     * @param cooldown the interaction cooldown, cleared for leaving players
     */
    private class Lifecycle(private val cooldown: InteractCooldown) : Listener {
        /** Shows the joining player the NPCs everyone sees; the rest follow once the progression is loaded. */
        @EventHandler(priority = EventPriority.MONITOR)
        fun onJoin(event: PlayerJoinEvent) {
            refresh(event.player)
        }

        /** Removes the leaving player from the viewers of every NPC and clears their cooldown. */
        @EventHandler(priority = EventPriority.MONITOR)
        fun onQuit(event: PlayerQuitEvent) {
            visibility?.hideAll(event.player)
            cooldown.forget(event.player.uniqueId)
        }
    }
}
