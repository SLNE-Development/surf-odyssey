package dev.slne.surf.odyssey.runtime.paper.hub.skillworld

object SkillWorldRules {
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
