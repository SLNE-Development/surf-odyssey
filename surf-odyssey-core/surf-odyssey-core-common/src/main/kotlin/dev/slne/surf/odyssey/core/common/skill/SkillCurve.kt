package dev.slne.surf.odyssey.core.common.skill

import kotlin.math.pow

object SkillCurve {
    const val MAX_LEVEL = 50

    private const val BASE_XP = 1500.0

    private const val GROWTH = 1.40

    val MAX_LEVEL_XP: Long = xpForLevel(MAX_LEVEL)

    fun xpForLevel(level: Int): Long {
        require(level in 0..MAX_LEVEL) { "level must be in 0..$MAX_LEVEL but was $level" }
        return (BASE_XP * (GROWTH.pow(level) - 1.0)).toLong()
    }

    fun levelFor(xp: Long): Int {
        var level = 0
        while (level < MAX_LEVEL && xp >= xpForLevel(level + 1)) {
            level++
        }
        return level
    }

    fun progressToNextLevel(xp: Long): Double {
        val level = levelFor(xp)
        if (level >= MAX_LEVEL) return 1.0

        val levelStart = xpForLevel(level)
        return (xp - levelStart).toDouble() / (xpForLevel(level + 1) - levelStart)
    }

    fun clamp(xp: Long): Long = xp.coerceAtLeast(0L)
}
