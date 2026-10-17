package dev.slne.surf.odyssey.runtime.paper.hub.skillworld

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.plugin
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.SkillWorldSettings
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.effectiveOres
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.parseNames
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal.Portals
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.region.SkillRegions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.bukkit.Bukkit
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.WorldCreator
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

object SkillWorld {
    private val lifecycles = mutableListOf<SkillWorldLifecycle>()
    private val LOCK_WAIT = 2.seconds
    private val mutex = Mutex()

    @Volatile
    private var shuttingDown = false

    sealed interface State {
        data object NotLoaded : State

        data object Loading : State

        data object BuildMode : State

        data class Loaded(val world: World) : State

        data class Unloading(val world: World) : State

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

        BuildMode,

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
        return when (state) {
            is State.Loaded -> ReloadResult.Reloaded
            State.BuildMode -> ReloadResult.BuildMode
            else -> ReloadResult.LoadFailed
        }
    }

    fun hubWorld(): World = Bukkit.getWorlds().first()

    fun hubSpawn(): Location = hubWorld().spawnLocation

    fun isSkillWorld(world: World): Boolean = world == this.world

    fun isEditWorld(world: World): Boolean =
        isSkillWorld(world) || world.name == SkillWorldRules.BUILD_WORLD_NAME || world.name == SkillWorldSettings.config.worldName.trim()

    fun openEditWorld(): World {
        val name = SkillWorldSettings.config.worldName.trim()
        return if (name.isNotEmpty() && modeOf(name) == SkillWorldRules.WorldMode.Live) {
            Bukkit.getWorld(name) ?: voidWorld(name)
        } else {
            openBuildWorld()
        }
    }

    fun openBuildWorld(): World {
        Bukkit.getWorld(SkillWorldRules.BUILD_WORLD_NAME)?.let { return it }

        val fresh = !WorldFolders.isWorld(worldFolder(SkillWorldRules.BUILD_WORLD_NAME))
        val world = voidWorld(SkillWorldRules.BUILD_WORLD_NAME)
        if (fresh) {
            for (x in -2..2) for (z in -2..2) world.getBlockAt(x, 63, z).type = Material.STONE
        }
        return world
    }

    fun allowsXp(skill: SkillType, location: Location): Boolean {
        val world = world ?: return false
        if (location.world != world) return false

        return regions.allows(skill, location.blockX, location.blockY, location.blockZ)
    }

    private fun worldContainer(): Path = Bukkit.getWorldContainer().toPath()

    private fun worldFolder(name: String): Path {
        val dimensions = worldContainer().resolve(hubWorld().name).resolve("dimensions").resolve("minecraft")
        return if (Files.isDirectory(dimensions)) dimensions.resolve(name) else worldContainer().resolve(name)
    }

    private fun modeOf(name: String): SkillWorldRules.WorldMode =
        SkillWorldRules.worldMode(name, name.isNotEmpty() && WorldFolders.isWorld(worldFolder(name)), setOf(hubWorld().name))

    private fun voidWorld(name: String): World =
        WorldCreator(name).generator(VoidChunkGenerator()).generateStructures(false).createWorld()
            ?: error("Bukkit could not load the world $name")

    private suspend fun loadLocked() {
        if (state is State.Loaded || shuttingDown) return

        val name = SkillWorldSettings.config.worldName.trim()
        when (modeOf(name)) {
            SkillWorldRules.WorldMode.Invalid -> fail(IllegalStateException("world-name \"$name\" in skill-world.yml is not allowed"))
            SkillWorldRules.WorldMode.Build -> enterBuildMode(name)
            SkillWorldRules.WorldMode.Live -> loadLive(name)
        }
    }

    private fun enterBuildMode(configuredName: String) {
        try {
            openBuildWorld()
        } catch (e: Exception) {
            fail(e)
            return
        }

        state = State.BuildMode
        if (configuredName.isEmpty()) {
            plugin.logger.info("No world-name in skill-world.yml, the build world ${SkillWorldRules.BUILD_WORLD_NAME} is active and portals stay disabled")
        } else {
            plugin.logger.warning("The world folder $configuredName from skill-world.yml does not exist, the build world is active and portals stay disabled")
        }
    }

    private suspend fun loadLive(name: String) {
        val runtimeName = SkillWorldRules.runtimeName(name)
        state = State.Loading

        val runtimeWorld = try {
            check(Bukkit.getWorld(runtimeName) == null) { "The world $runtimeName is still loaded" }
            Bukkit.getWorld(name)?.save()
            withContext(Dispatchers.IO) { WorldFolders.replace(worldFolder(name), worldFolder(runtimeName)) }
            voidWorld(runtimeName)
        } catch (e: Throwable) {
            if (e is CancellationException) {
                state = State.NotLoaded
                throw e
            }
            fail(e)
            return
        }

        if (shuttingDown) {
            Bukkit.unloadWorld(runtimeWorld, false)
            state = State.NotLoaded
            return
        }

        runtimeWorld.isAutoSave = false
        runCatching { runtimeWorld.setGameRule(GameRules.SPAWN_MOBS, false) }
            .onFailure { plugin.logger.log(Level.SEVERE, "Failed to disable natural mob spawning in the skill world", it) }
        state = State.Loaded(runtimeWorld)
        plugin.logger.info("Loaded skill world ${runtimeWorld.name} as a copy of $name")
        notifyLoaded(runtimeWorld)
    }

    private fun notifyLoaded(world: World) {
        lifecycles.forEach { lifecycle ->
            runCatching { lifecycle.onLoaded(world) }
                .onFailure { plugin.logger.log(Level.SEVERE, "A skill world lifecycle failed on load", it) }
        }
    }

    private suspend fun unloadLocked(): Boolean {
        val tracked = when (val current = state) {
            is State.Loaded -> current.world
            is State.Unloading -> current.world.takeIf { Bukkit.getWorld(it.uid) != null }
            else -> null
        }
        if (tracked == null) {
            state = State.NotLoaded
            return true
        }
        state = State.Unloading(tracked)

        lifecycles.forEach { lifecycle ->
            runCatching { lifecycle.onUnloading(tracked) }
                .onFailure { plugin.logger.log(Level.SEVERE, "A skill world lifecycle failed on unload", it) }
        }

        val spawn = hubSpawn()
        tracked.players.forEach { player ->
            runCatching {
                if (!player.teleport(spawn)) {
                    plugin.logger.warning("Could not teleport ${player.name} out of the skill world")
                }
            }.onFailure { plugin.logger.log(Level.SEVERE, "Failed to teleport ${player.name} out of the skill world", it) }
        }

        val requested = runCatching { Bukkit.unloadWorld(tracked, false) }
            .onFailure { plugin.logger.log(Level.SEVERE, "Failed to unload the skill world, it stays tracked and protected", it) }
            .getOrDefault(false)
        val unloaded = settleUnload(tracked, requested)
        if (unloaded) {
            withContext(Dispatchers.IO) {
                runCatching { WorldFolders.delete(worldFolder(tracked.name)) }
                    .onFailure { plugin.logger.log(Level.WARNING, "Could not delete the folder of the skill world copy ${tracked.name}", it) }
            }
        }
        return unloaded
    }

    private fun settleUnload(bukkitWorld: World, requested: Boolean): Boolean {
        return when (SkillWorldRules.unloadOutcome(Bukkit.getWorld(bukkitWorld.uid) != null)) {
            SkillWorldRules.UnloadOutcome.Gone -> {
                if (!requested) plugin.logger.severe("Bukkit reported a failed unload of the skill world, but the world is gone")
                state = State.NotLoaded
                true
            }

            SkillWorldRules.UnloadOutcome.StillPresent -> {
                state = State.Loaded(bukkitWorld)
                false
            }
        }
    }

    private fun fail(cause: Throwable) {
        state = State.Failed(cause)
        plugin.logger.log(Level.SEVERE, "Failed to load the skill world, portals stay disabled", cause)
    }
}
