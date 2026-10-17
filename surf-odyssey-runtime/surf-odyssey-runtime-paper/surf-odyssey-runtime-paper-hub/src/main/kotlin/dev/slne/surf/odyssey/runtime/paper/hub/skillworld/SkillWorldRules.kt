package dev.slne.surf.odyssey.runtime.paper.hub.skillworld

object SkillWorldRules {
    const val BUILD_WORLD_NAME = "skill_world"
    private const val RUNTIME_SUFFIX = "_runtime"
    private val WORLD_NAME = Regex("[A-Za-z0-9_-]{1,32}")

    enum class WorldMode {
        Build,

        Live,

        Invalid,
    }

    fun runtimeName(sourceName: String): String = sourceName + RUNTIME_SUFFIX

    fun worldMode(configuredName: String?, sourceExists: Boolean, reservedNames: Set<String>): WorldMode {
        val name = configuredName?.trim().orEmpty()
        if (name.isEmpty()) return WorldMode.Build
        if (!WORLD_NAME.matches(name) || name.endsWith(RUNTIME_SUFFIX) || name in reservedNames) return WorldMode.Invalid

        return if (sourceExists) WorldMode.Live else WorldMode.Build
    }

    fun canBreak(inMiningRegion: Boolean, unbreakable: Boolean): Boolean = inMiningRegion && !unbreakable

    fun mustCancelBucket(inSkillWorld: Boolean): Boolean = inSkillWorld

    fun mustCancelCreatureSpawn(inSkillWorld: Boolean, custom: Boolean): Boolean = inSkillWorld && !custom

    enum class UnloadOutcome {
        Gone,

        StillPresent,
    }

    fun unloadOutcome(worldStillInBukkit: Boolean): UnloadOutcome =
        if (worldStillInBukkit) UnloadOutcome.StillPresent else UnloadOutcome.Gone

    fun mustEjectFromLockedRegion(locked: Boolean, inRegion: Boolean): Boolean = locked && inRegion
}
