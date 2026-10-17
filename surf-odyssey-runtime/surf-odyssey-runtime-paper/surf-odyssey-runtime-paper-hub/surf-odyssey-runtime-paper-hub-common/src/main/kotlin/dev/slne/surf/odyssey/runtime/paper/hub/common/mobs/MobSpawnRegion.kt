package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.EntityType
import java.util.UUID
import kotlin.time.Duration

/**
 * A region that keeps up to [maxAlive] mobs of [types] alive, spawning one at a random
 * [spawnPoints] entry every [respawnInterval]. Spawned mobs do not persist across restarts.
 *
 * @property types entity types to pick from
 * @property spawnPoints blocks mobs spawn on, all inside the shape
 * @property respawnInterval time between two spawns
 * @property maxAlive most mobs alive at once
 */
class MobSpawnRegion(
    id: String,
    displayName: Component,
    shape: Shape,
    val types: List<EntityType>,
    val spawnPoints: List<BlockPoint>,
    val respawnInterval: Duration,
    val maxAlive: Int,
) : Region(id, displayName, shape) {
    init {
        require(types.isNotEmpty()) { "Mob region '$id' needs at least one mob type" }
        require(spawnPoints.isNotEmpty()) { "Mob region '$id' needs at least one spawn point" }
        require(maxAlive > 0) { "Mob region '$id' needs a positive maximum" }
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
        val point = spawnPoints.random()
        val location = Location(world, point.x + 0.5, point.y.toDouble(), point.z + 0.5)
        if (!location.isChunkLoaded) return
        val entity = world.spawnEntity(location, types.random())
        entity.isPersistent = false
        alive.add(entity.uniqueId)
        lastSpawnAt = now
    }
}
