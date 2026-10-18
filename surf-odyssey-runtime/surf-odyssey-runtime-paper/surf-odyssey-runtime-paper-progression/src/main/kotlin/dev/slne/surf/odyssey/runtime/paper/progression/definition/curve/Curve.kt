package dev.slne.surf.odyssey.runtime.paper.progression.definition.curve

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

/** Maps a tool level to the total usage needed to reach it. */
fun interface Curve {
    /**
     * Returns the total usage needed to reach [level]; level 1 needs no usage.
     *
     * @param level the tool level, starting at 1
     */
    fun usageFor(level: Int): Long
}

/** Building blocks for [Curve]s and the curves shared by the tool types. */
object Curves {
    /**
     * Curve that grows with the square of the level: `maxUsage * ((L - 1) / (maxLevel - 1))^2`, rounded.
     *
     * @param maxUsage usage needed for [maxLevel]
     * @param maxLevel the level that needs [maxUsage]
     */
    fun quadratic(maxUsage: Long, maxLevel: Int = 10): Curve {
        require(maxLevel >= 2) { "maxLevel must be at least 2" }
        return Curve { level ->
            if (level <= 1) 0 else (maxUsage * ((level - 1).toDouble() / (maxLevel - 1)).pow(2)).roundToLong()
        }
    }

    /**
     * Curve whose increments shrink with the level:
     * `maxUsage * ln(1 + steepness * t) / ln(1 + steepness)` with `t = (L - 1) / (maxLevel - 1)`, rounded.
     * A higher [steepness] front-loads more of the usage onto the early levels.
     *
     * @param maxUsage usage needed for [maxLevel]
     * @param steepness positive shape factor
     * @param maxLevel the level that needs [maxUsage]
     */
    fun logCurve(maxUsage: Long, steepness: Double, maxLevel: Int = 10): Curve {
        require(steepness > 0) { "steepness must be positive" }
        require(maxLevel >= 2) { "maxLevel must be at least 2" }
        val norm = ln(1 + steepness)
        return Curve { level ->
            if (level <= 1) {
                0
            } else {
                val t = (level - 1).toDouble() / (maxLevel - 1)
                (maxUsage * ln(1 + steepness * t) / norm).roundToLong()
            }
        }
    }

    /**
     * Curve whose increments grow geometrically: reaching level 2 needs [base], and each further level needs
     * [factor] times the increment of the level before. The total is `base * (factor^(L - 1) - 1) / (factor - 1)`,
     * rounded.
     *
     * @param base usage needed for level 2
     * @param factor growth factor of the increments, greater than 1
     */
    fun exponential(base: Long, factor: Double): Curve {
        require(factor > 1) { "factor must be greater than 1" }
        return Curve { level ->
            if (level <= 1) 0 else (base * (factor.pow(level - 1) - 1) / (factor - 1)).roundToLong()
        }
    }

    /**
     * Curve that needs the same usage for every level: `perLevel * (L - 1)`.
     *
     * @param perLevel usage needed per level
     */
    fun linear(perLevel: Long): Curve = Curve { level -> if (level <= 1) 0 else perLevel * (level - 1) }

    /**
     * Returns the highest level in `1..maxLevel` whose usage is reached by [usage].
     *
     * @param curve the curve of the tool
     * @param usage the total usage of the tool
     * @param maxLevel the highest level of the tool
     */
    fun levelFor(curve: Curve, usage: Long, maxLevel: Int): Int {
        var level = 1
        while (level < maxLevel && curve.usageFor(level + 1) <= usage) level++
        return level
    }

    /** Curve of pickaxes: quadratic up to 100 000 usage at level 10. */
    val PICKAXE: Curve = quadratic(100_000)

    /** Curve of axes: quadratic up to 100 000 usage at level 10. */
    val AXE: Curve = quadratic(100_000)

    /** Curve of fishing rods: quadratic up to 100 000 usage at level 10. */
    val ROD: Curve = quadratic(100_000)

    /** Curve of swords and bows: quadratic up to 100 000 usage at level 10. */
    val WEAPON: Curve = quadratic(100_000)
}
