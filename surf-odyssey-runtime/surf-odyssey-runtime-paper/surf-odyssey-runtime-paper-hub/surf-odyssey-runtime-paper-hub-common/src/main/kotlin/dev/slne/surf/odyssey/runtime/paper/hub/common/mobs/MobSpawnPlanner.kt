package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

/** Decides how many mobs a spawn region adds now. */
object MobSpawnPlanner {
    /** Returns 1 if the interval since [lastSpawnAt] has passed and fewer than [maxAlive] mobs live, else 0. */
    fun toSpawn(alive: Int, maxAlive: Int, lastSpawnAt: Long, now: Long, intervalMillis: Long): Int =
        if (alive < maxAlive && now - lastSpawnAt >= intervalMillis) 1 else 0
}
