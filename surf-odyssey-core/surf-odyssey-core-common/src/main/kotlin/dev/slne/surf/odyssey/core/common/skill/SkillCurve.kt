package dev.slne.surf.odyssey.core.common.skill

import kotlin.math.pow

object SkillCurve {
    const val MAX_LEVEL = 25

    private const val BASE_XP = 1500.0
    private const val GROWTH = 1.40

    val MAX_XP: Long = xpForLevel(MAX_LEVEL)

    fun xpForLevel(level: Int): Long {
        require(level in 1..MAX_LEVEL) { "level must be in 1..$MAX_LEVEL but was $level" }
        return (BASE_XP * (GROWTH.pow(level - 1) - 1.0)).toLong()
    }

    fun levelFor(xp: Long): Int {
        var level = 1
        while (level < MAX_LEVEL && xp >= xpForLevel(level + 1)) {
            level++
        }
        return level
    }

    fun clamp(xp: Long): Long = xp.coerceIn(0L, MAX_XP)
}
