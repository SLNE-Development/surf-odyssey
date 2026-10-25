package dev.slne.surf.odyssey.runtime.paper.polartest.session

import dev.slne.surf.odyssey.runtime.paper.polartest.PolarTestPaperMain
import dev.slne.surf.odyssey.runtime.paper.polartest.data.MarkedPositionsRegistry
import dev.slne.surf.polarpaper.api.SurfPolarPaperApi
import dev.slne.surf.polarpaper.api.world.SurfWorld
import kotlinx.coroutines.future.await
import org.bukkit.Bukkit
import org.bukkit.World
import java.util.UUID

/**
 * Returns [uuid], or throws when no test world was set up yet.
 *
 * @throws IllegalStateException naming the setup step when [uuid] is `null`
 */
fun requireTestWorld(uuid: UUID?): UUID = uuid ?: throw IllegalStateException("Keine Testwelt angelegt, zuerst /polartest setup")

/**
 * Holds the API instance that owns [MAIN_GAME_MODE] and the main test world. Opening a version closes the previous
 * instance first, the way an updated plugin build replaces the old one.
 */
class PolarTestSession(private val plugin: PolarTestPaperMain) {
    private var api: SurfPolarPaperApi? = null

    /** The uuid of the main test world, or `null` before the first setup. */
    var worldUuid: UUID? = null

    /** The version the main game mode was last opened at. */
    var version: Int = 1
        private set

    /** The main test world when the current instance has it loaded. */
    val loadedMainWorld: SurfWorld? get() = worldUuid?.let { api?.findWorld(it, MAIN_GAME_MODE) }

    /** Closes the current instance and creates a new one that owns [MAIN_GAME_MODE] at [version]. */
    suspend fun reopen(version: Int): SurfPolarPaperApi {
        closeApi()
        val created = SurfPolarPaperApi.create(plugin, mainSettings(version))
        api = created
        this.version = version
        return created
    }

    /** Reopens at [version] and loads the main test world, which runs the migrations up to [version]. */
    suspend fun openMainWorld(version: Int = this.version): SurfWorld =
        reopen(version).loadWorld(requireTestWorld(worldUuid), MAIN_GAME_MODE)

    /** Deletes the main test world, loading it first when needed, and forgets its uuid. */
    suspend fun deleteMainWorld() {
        if (worldUuid == null) return
        val world = loadedMainWorld ?: openMainWorld()
        evacuate(world.bukkitWorld)
        world.delete()
        worldUuid = null
    }

    /**
     * Moves every player out of the worlds of the current instance, saves and unloads them and closes the instance.
     * Players have to leave first: Bukkit refuses to unload a world with players, which would keep it locked.
     */
    suspend fun closeApi() {
        val current = api ?: return
        api = null
        for (world in current.loadedWorlds.toList()) evacuate(world.bukkitWorld)
        try {
            current.unloadAllWorlds()
        } finally {
            current.close()
            MarkedPositionsRegistry.clear()
        }
    }

    /** Teleports every player in [world] to the spawn of the server's default world. */
    suspend fun evacuate(world: World) {
        val teleports = plugin.onMain {
            val target = Bukkit.getWorlds().first().spawnLocation
            world.players.map { it.teleportAsync(target) }
        }
        teleports.forEach { it.await() }
    }
}
