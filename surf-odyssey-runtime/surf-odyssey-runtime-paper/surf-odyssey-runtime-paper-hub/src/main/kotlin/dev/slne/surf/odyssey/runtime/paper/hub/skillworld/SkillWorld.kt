package dev.slne.surf.odyssey.runtime.paper.hub.skillworld

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.plugin
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.SkillWorldSettings
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.effectiveOres
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.parseNames
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal.Portals
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.region.SkillRegions
import dev.slne.surf.polarpaper.api.SurfPolarPaperApi
import dev.slne.surf.polarpaper.api.world.SurfWorld
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.bukkit.Bukkit
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import java.util.UUID
import java.util.logging.Level
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

object SkillWorld {
    private val LOAD_TIMEOUT = 30.seconds
    private val UNLOAD_TIMEOUT = 10.seconds
    private val lifecycles = mutableListOf<SkillWorldLifecycle>()
    private val LOCK_WAIT = 2.seconds
    private val mutex = Mutex()

    @Volatile
    private var shuttingDown = false

    sealed interface State {
        data object NotLoaded : State

        data object Loading : State

        data class Loaded(val surfWorld: SurfWorld, val world: World) : State

        data class Unloading(val surfWorld: SurfWorld, val world: World) : State

        data class Failed(val cause: Throwable) : State
    }

    @Volatile
    var state: State = State.NotLoaded
        private set

    val world: World? get() = (state as? State.Loaded)?.world

    @Volatile
    var regions: SkillRegions = SkillRegions(emptyMap())
        private set

    @Volatile
    var carrierBlocks: Set<Material> = emptySet()
        private set

    @Volatile
    var unbreakableBlocks: Set<Material> = emptySet()
        private set

    fun register(lifecycle: SkillWorldLifecycle) {
        lifecycles += lifecycle
    }

    fun refreshRegions() {
        val config = SkillWorldSettings.config
        regions = SkillRegions.from(config)
        carrierBlocks = parseNames(
            config.mining.effectiveOres().keys,
            Material::matchMaterial,
            { plugin.logger.warning("Unknown carrier block $it in skill-world.yml") },
        ).toSet()
        unbreakableBlocks = parseNames(
            config.mining.unbreakableBlocks,
            Material::matchMaterial,
            { plugin.logger.warning("Unknown unbreakable block $it in skill-world.yml") },
        ).toSet()
    }

    suspend fun load() = mutex.withLock { withContext(plugin.globalRegionDispatcher) { loadLocked() } }

    suspend fun unload() = mutex.withLock { withContext(plugin.globalRegionDispatcher) { unloadLocked() } }

    suspend fun shutdown() {
        shuttingDown = true
        val locked = withTimeoutOrNull(LOCK_WAIT) {
            mutex.lock()
            true
        } ?: false

        if (locked) {
            try {
                unloadOnServerThread()
            } finally {
                mutex.unlock()
            }
        } else {
            unloadOnServerThread()
        }
    }

    private suspend fun unloadOnServerThread() {
        if (Bukkit.isPrimaryThread()) unloadForShutdown() else withContext(plugin.globalRegionDispatcher) { unloadForShutdown() }
    }

    private suspend fun unloadForShutdown() {
        val tracked = when (val current = state) {
            is State.Loaded -> current.world
            is State.Unloading -> current.world
            else -> null
        }

        unloadLocked()

        if (tracked != null && Bukkit.getWorld(tracked.uid) != null) {
            runCatching { Bukkit.unloadWorld(tracked, false) }
                .onSuccess { plugin.logger.log(Level.SEVERE, "The skill world was still loaded at shutdown, unloaded it directly (result: $it)") }
                .onFailure { plugin.logger.log(Level.SEVERE, "Failed to unload the skill world directly at shutdown", it) }
        }
    }

    enum class ReloadResult {
        Reloaded,

        ConfigInvalid,

        UnloadFailed,

        LoadFailed,
    }

    suspend fun reload(): ReloadResult = mutex.withLock { withContext(plugin.globalRegionDispatcher) { reloadLocked() } }

    private suspend fun reloadLocked(): ReloadResult {
        try {
            SkillWorldSettings.reload()
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to read skill-world.yml, keeping the current skill world", e)
            return ReloadResult.ConfigInvalid
        }
        if (!SkillWorldBootstrap.setup()) return ReloadResult.ConfigInvalid

        val unloaded = unloadLocked()
        refreshRegions()
        Portals.refresh()
        if (!unloaded) {
            (state as? State.Loaded)?.let { notifyLoaded(it.world) }
            return ReloadResult.UnloadFailed
        }

        loadLocked()
        return if (state is State.Loaded) ReloadResult.Reloaded else ReloadResult.LoadFailed
    }

    fun hubWorld(): World = Bukkit.getWorlds().first()

    fun hubSpawn(): Location = hubWorld().spawnLocation

    fun isSkillWorld(world: World): Boolean = world == this.world

    fun allowsXp(skill: SkillType, location: Location): Boolean {
        val world = world ?: return false
        if (location.world != world) return false

        return regions.allows(skill, location.blockX, location.blockY, location.blockZ)
    }

    private suspend fun loadLocked() {
        if (state is State.Loaded || shuttingDown) return

        val config = SkillWorldSettings.config
        val uuid = config.worldUuid?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (uuid == null) {
            fail(IllegalStateException("world-uuid in skill-world.yml is missing or invalid"))
            return
        }

        state = State.Loading
        var surfWorld: SurfWorld? = null
        val loaded = try {
            val polarWorld = withTimeout(LOAD_TIMEOUT) { SurfPolarPaperApi.loadWorld(uuid, config.gameModeIdentifier) }
            surfWorld = polarWorld
            if (shuttingDown) {
                withContext(NonCancellable) { unloadWithoutSaving(polarWorld) }
                state = State.NotLoaded
                return
            }
            val world = polarWorld.platformWorld<World>() ?: error("The loaded skill world has no platform world")
            State.Loaded(polarWorld, world)
        } catch (e: Throwable) {
            withContext(NonCancellable) { discardUntracked(surfWorld, uuid, config.gameModeIdentifier) }
            if (e.isRealCancellation()) {
                state = State.NotLoaded
                throw e
            }
            fail(e)
            return
        }

        runCatching { loaded.world.setGameRule(GameRules.SPAWN_MOBS, false) }
            .onFailure { plugin.logger.log(Level.SEVERE, "Failed to disable natural mob spawning in the skill world", it) }
        state = loaded
        plugin.logger.info("Loaded skill world ${loaded.world.name}")
        notifyLoaded(loaded.world)
    }

    private fun notifyLoaded(world: World) {
        lifecycles.forEach { lifecycle ->
            runCatching { lifecycle.onLoaded(world) }
                .onFailure { plugin.logger.log(Level.SEVERE, "A skill world lifecycle failed on load", it) }
        }
    }

    private suspend fun unloadLocked(): Boolean {
        val tracked = when (val current = state) {
            is State.Loaded -> current.surfWorld to current.world
            is State.Unloading -> (current.surfWorld to current.world).takeIf { Bukkit.getWorld(current.world.uid) != null }
            else -> null
        }
        if (tracked == null) {
            state = State.NotLoaded
            return true
        }
        val (surfWorld, bukkitWorld) = tracked
        state = State.Unloading(surfWorld, bukkitWorld)

        lifecycles.forEach { lifecycle ->
            runCatching { lifecycle.onUnloading(bukkitWorld) }
                .onFailure { plugin.logger.log(Level.SEVERE, "A skill world lifecycle failed on unload", it) }
        }

        val spawn = hubSpawn()
        bukkitWorld.players.forEach { player ->
            runCatching {
                if (!player.teleport(spawn)) {
                    plugin.logger.warning("Could not teleport ${player.name} out of the skill world")
                }
            }.onFailure { plugin.logger.log(Level.SEVERE, "Failed to teleport ${player.name} out of the skill world", it) }
        }

        val polarUnloaded = try {
            unloadWithoutSaving(surfWorld)
        } catch (e: Throwable) {
            settleUnload(surfWorld, bukkitWorld, false)
            throw e
        }
        return settleUnload(surfWorld, bukkitWorld, polarUnloaded)
    }

    private fun settleUnload(surfWorld: SurfWorld, bukkitWorld: World, polarUnloaded: Boolean): Boolean {
        return when (SkillWorldRules.unloadOutcome(Bukkit.getWorld(bukkitWorld.uid) != null)) {
            SkillWorldRules.UnloadOutcome.Gone -> {
                if (!polarUnloaded) plugin.logger.severe("Polar reported a failed unload of the skill world, but the world is gone")
                state = State.NotLoaded
                true
            }

            SkillWorldRules.UnloadOutcome.StillPresent -> {
                state = State.Loaded(surfWorld, bukkitWorld)
                false
            }
        }
    }

    private suspend fun discardUntracked(known: SurfWorld?, uuid: UUID, gameModeIdentifier: String) {
        val leaked = known ?: runCatching { SurfPolarPaperApi.findWorld(uuid, gameModeIdentifier) }.getOrNull() ?: return
        unloadWithoutSaving(leaked)
    }

    private suspend fun unloadWithoutSaving(surfWorld: SurfWorld): Boolean {
        try {
            withTimeout(UNLOAD_TIMEOUT) { SurfPolarPaperApi.unloadWorld(surfWorld, false) }
            return true
        } catch (e: Throwable) {
            if (e.isRealCancellation()) throw e
            plugin.logger.log(Level.SEVERE, "Failed to unload the skill world, it stays tracked and protected", e)
            return false
        }
    }

    private fun Throwable.isRealCancellation() = this is CancellationException && this !is TimeoutCancellationException

    private fun fail(cause: Throwable) {
        state = State.Failed(cause)
        plugin.logger.log(Level.SEVERE, "Failed to load the skill world, portals stay disabled", cause)
    }
}
