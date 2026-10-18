package dev.slne.surf.odyssey.runtime.paper.hub.common.progression

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/** Decides whether the double drop bonus of a tool doubles one harvest or catch. */
object DoubleDrop {
    /**
     * Returns how often the drops of one harvest or catch drop: twice if [roll] lies below [chance], else once. A
     * chance of 0 or less never doubles.
     *
     * @param chance the double drop chance of the tool, between 0 and 1
     * @param roll a random number in `[0, 1)`
     */
    fun times(chance: Double, roll: Double): Int = if (chance > 0.0 && roll < chance) 2 else 1

    /**
     * Returns how often the drops of one harvest or catch with a tool of double drop [chance] drop, rolled with
     * [random].
     *
     * @param chance the double drop chance of the tool, between 0 and 1
     * @param random the source of the roll
     */
    fun roll(chance: Double, random: Random = Random.Default): Int = times(chance, random.nextDouble())
}

/**
 * The harvested blocks whose vanilla drops are to drop twice, from the break until their drops are spawned. Safe for
 * use from several threads.
 */
class PendingDoubleDrops {
    /** The marked blocks. */
    private val points: MutableSet<BlockPoint> = ConcurrentHashMap.newKeySet()

    /** Marks the block at [point] so that its next drops are doubled. */
    fun mark(point: BlockPoint) {
        points += point
    }

    /**
     * Removes the mark of the block at [point].
     *
     * @return whether the block was marked, so that its drops are to be doubled
     */
    fun take(point: BlockPoint): Boolean = points.remove(point)

    /** Removes the mark of the block at [point] without using it. */
    fun forget(point: BlockPoint) {
        points -= point
    }
}
