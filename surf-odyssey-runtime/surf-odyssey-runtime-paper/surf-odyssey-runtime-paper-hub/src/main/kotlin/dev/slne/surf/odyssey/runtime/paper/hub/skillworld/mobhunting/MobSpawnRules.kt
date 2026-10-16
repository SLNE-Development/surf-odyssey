package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mobhunting

object MobSpawnRules {
    fun missing(alive: Int, max: Int): Int = (max - alive).coerceAtLeast(0)

    fun mustReturn(inRegion: Boolean): Boolean = !inRegion

    fun canSpawnAt(chunkLoaded: Boolean, entitiesLoaded: Boolean): Boolean = chunkLoaded && entitiesLoaded
}
