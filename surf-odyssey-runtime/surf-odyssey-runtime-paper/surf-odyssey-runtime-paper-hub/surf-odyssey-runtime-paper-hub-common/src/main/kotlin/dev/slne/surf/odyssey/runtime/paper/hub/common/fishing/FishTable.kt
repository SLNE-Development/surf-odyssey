package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * What is known about the spot where a fish bites.
 *
 * @property raining whether it rains at the spot
 * @property thundering whether a thunderstorm is going on
 * @property night whether it is night
 * @property biome biome key like `minecraft:river`
 * @property waterDepth number of water blocks below and including the hook block
 */
data class FishContext(val raining: Boolean, val thundering: Boolean, val night: Boolean, val biome: String, val waterDepth: Int)

/** A requirement a fish has on the spot it bites at. */
fun interface FishCondition {
    /** Returns whether the fish can bite in [context]. */
    fun test(context: FishContext): Boolean

    companion object {
        /** Bites only while it rains. */
        fun rain() = FishCondition { it.raining }

        /** Bites only during a thunderstorm. */
        fun thunder() = FishCondition { it.thundering }

        /** Bites only at night. */
        fun night() = FishCondition { it.night }

        /** Bites only by day. */
        fun day() = FishCondition { !it.night }

        /** Bites only in one of the biomes with the given [keys]. */
        fun biome(vararg keys: String) = FishCondition { it.biome in keys }

        /** Bites only where the water is at least [blocks] deep. */
        fun minDepth(blocks: Int) = FishCondition { it.waterDepth >= blocks }
    }
}

/**
 * A fish with its relative weight, its conditions and the item a catch gives.
 *
 * @property id id of the fish
 * @property weight relative chance of being caught, positive
 * @property conditions requirements on the spot, all of which must hold
 * @property item creates the item a catch gives
 */
class FishEntry(val id: String, val weight: Int, val conditions: List<FishCondition>, val item: () -> ItemStack) {
    init {
        require(weight > 0) { "Fish '$id' needs a positive weight" }
    }

    /** Returns whether every condition holds in [context]. */
    fun fits(context: FishContext): Boolean = conditions.all { it.test(context) }
}

/**
 * The fish a fishing region can give, picked by weight among those that fit.
 *
 * @property entries all fish of the table
 */
class FishTable(val entries: List<FishEntry>) {
    /** Picks a fish that fits [context], or returns `null` if none does. */
    fun roll(context: FishContext, random: Random): FishEntry? {
        val candidates = entries.filter { it.fits(context) }
        if (candidates.isEmpty()) return null
        var pick = random.nextInt(candidates.sumOf { it.weight })
        for (candidate in candidates) {
            pick -= candidate.weight
            if (pick < 0) return candidate
        }
        return candidates.last()
    }
}
