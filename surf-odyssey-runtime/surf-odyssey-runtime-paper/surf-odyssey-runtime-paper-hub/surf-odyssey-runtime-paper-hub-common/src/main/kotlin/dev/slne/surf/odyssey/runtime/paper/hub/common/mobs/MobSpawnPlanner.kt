package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint

/** Decides how many mobs a spawn region adds now and where they appear. */
object MobSpawnPlanner {
    /** Returns 1 if the interval since [lastSpawnAt] has passed and fewer than [maxAlive] mobs live, else 0. */
    fun toSpawn(alive: Int, maxAlive: Int, lastSpawnAt: Long, now: Long, intervalMillis: Long): Int =
        if (alive < maxAlive && now - lastSpawnAt >= intervalMillis) 1 else 0

    /** Returns the x, y and z a mob spawns at: centred on top of the block at [point]. */
    fun spawnPosition(point: BlockPoint): Triple<Double, Double, Double> = Triple(point.x + 0.5, point.y + 1.0, point.z + 0.5)
}
