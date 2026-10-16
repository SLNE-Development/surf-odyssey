package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mobhunting

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.plugin
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorld
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorldLifecycle
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.SkillWorldSettings
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.parseNames
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockBox
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.event.entity.CreatureSpawnEvent
import java.util.UUID
import java.util.logging.Level
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object MobHuntingSpawner : SkillWorldLifecycle {
    private val tracked = HashMap<UUID, Location>()
    private val jobs = mutableListOf<Job>()

    override fun onLoaded(world: World) {
        stop()

        val box = SkillWorld.regions.box(SkillType.MOB_HUNTING) ?: run {
            plugin.logger.warning("No mob hunting region configured, the mob spawner is disabled")
            return
        }
        val config = SkillWorldSettings.config.mobHunting
        val types = parseNames(config.mobTypes, { name -> EntityType.entries.firstOrNull { it.name.equals(name, true) } }) {
            plugin.logger.warning("Unknown mob type $it in skill-world.yml")
        }.filter { type ->
            (type.isSpawnable && type.isAlive && Mob::class.java.isAssignableFrom(type.entityClass)).also {
                if (!it) plugin.logger.warning("Mob type ${type.name} in skill-world.yml cannot be spawned as a mob and is ignored")
            }
        }
        val points = config.spawnPoints
            .map { Location(world, it.x, it.y, it.z, it.yaw, it.pitch) }
            .filter { point ->
                box.contains(point.x, point.y, point.z).also {
                    if (!it) plugin.logger.warning("Mob spawn point ${point.x} ${point.y} ${point.z} is outside the mob hunting region and ignored")
                }
            }

        if (types.isEmpty() || points.isEmpty()) {
            plugin.logger.warning("Mob hunting needs at least one valid mob type and one spawn point inside its region")
            return
        }

        val interval = config.respawnIntervalSeconds.coerceAtLeast(1).seconds
        jobs += loop(interval) { refill(world, types, points, config.maxMobs) }
        jobs += loop(1.seconds) { leash(box) }
    }

    override fun onUnloading(world: World) {
        stop()
    }

    private fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        tracked.keys.mapNotNull { Bukkit.getEntity(it) }.forEach { it.remove() }
        tracked.clear()
    }

    private fun loop(interval: Duration, action: () -> Unit): Job = plugin.scope.launch(plugin.globalRegionDispatcher) {
        while (isActive) {
            try {
                action()
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "The mob hunting spawner failed", e)
            }
            delay(interval)
        }
    }

    private fun refill(world: World, types: List<EntityType>, points: List<Location>, max: Int) {
        tracked.keys.removeIf { Bukkit.getEntity(it)?.isValid != true }

        repeat(MobSpawnRules.missing(tracked.size, max)) {
            val point = points.random()
            val chunkX = point.blockX shr 4
            val chunkZ = point.blockZ shr 4
            val chunkLoaded = world.isChunkLoaded(chunkX, chunkZ)
            if (!MobSpawnRules.canSpawnAt(chunkLoaded, chunkLoaded && world.getChunkAt(chunkX, chunkZ, false).isEntitiesLoaded)) return@repeat
            val entity = world.spawnEntity(point, types.random(), CreatureSpawnEvent.SpawnReason.CUSTOM) { spawned ->
                spawned.isPersistent = false
                (spawned as? Mob)?.removeWhenFarAway = false
            }
            tracked[entity.uniqueId] = point
        }
    }

    private fun leash(box: BlockBox) {
        for ((uuid, point) in tracked) {
            val entity = Bukkit.getEntity(uuid) as? LivingEntity ?: continue
            val location = entity.location
            if (!entity.isValid) continue
            if (MobSpawnRules.mustReturn(box.contains(location.x, location.y, location.z)) && entity.teleport(point)) {
                (entity as? Mob)?.target = null
            }
        }
    }
}
