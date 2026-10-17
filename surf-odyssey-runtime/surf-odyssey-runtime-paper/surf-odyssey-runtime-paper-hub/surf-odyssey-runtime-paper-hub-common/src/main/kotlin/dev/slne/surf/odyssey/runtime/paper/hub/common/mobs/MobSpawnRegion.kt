package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.ProtectionSettings
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import java.util.UUID
import kotlin.time.Duration

/**
 * A region that keeps up to [maxAlive] mobs of [types] alive, spawning one on top of a random
 * [spawnPoints] entry every [respawnInterval]. Spawned mobs do not persist across restarts and
 * are removed when the hub plugin is disabled.
 *
 * @property types entity types to pick from, each a spawnable mob
 * @property spawnPoints blocks mobs spawn on, all inside the shape
 * @property respawnInterval time between two spawns, positive
 * @property maxAlive most mobs alive at once, at least 1
 */
class MobSpawnRegion(
    id: String,
    displayName: Component,
    shape: Shape,
    val types: List<EntityType>,
    val spawnPoints: List<BlockPoint>,
    val respawnInterval: Duration,
    val maxAlive: Int,
    settings: ProtectionSettings = ProtectionSettings(),
) : Region(id, displayName, shape, settings) {
    init {
        require(types.isNotEmpty()) { "Mob region '$id' needs at least one mob type" }
        require(spawnPoints.isNotEmpty()) { "Mob region '$id' needs at least one spawn point" }
        require(maxAlive > 0) { "Mob region '$id' needs a positive maximum" }
        require(respawnInterval.isPositive()) { "Mob region '$id' needs a positive respawn interval" }
        types.forEach {
            require(it.isSpawnable && it.entityClass?.let(Mob::class.java::isAssignableFrom) == true) {
                "Mob region '$id' can only spawn mobs, $it is none"
            }
        }
        spawnPoints.forEach {
            require(shape.contains(it.x, it.y, it.z)) { "Spawn point $it of mob region '$id' lies outside its shape" }
        }
    }

    private val alive = AliveMobs<UUID>()
    private var lastSpawnAt = 0L

    /** Removes dead mobs from the count and spawns a new one when the planner allows it. */
    override fun onTick() {
        val world = HubPlugin.instance.hubWorld
        alive.prune { world.getEntity(it)?.isValid == true }
        val now = System.currentTimeMillis()
        if (MobSpawnPlanner.toSpawn(alive.count, maxAlive, lastSpawnAt, now, respawnInterval.inWholeMilliseconds) == 0) return
        val (x, y, z) = MobSpawnPlanner.spawnPosition(spawnPoints.random())
        val location = Location(world, x, y, z)
        if (!location.isChunkLoaded) return
        val entity = world.spawnEntity(location, types.random())
        entity.isPersistent = false
        alive.add(entity.uniqueId)
        lastSpawnAt = now
    }

    /** Removes every mob this region spawned that is still in the world and stops counting them. */
    internal fun removeMobs() {
        val world = HubPlugin.instance.hubWorld
        alive.removeAll { world.getEntity(it)?.remove() }
    }
}
