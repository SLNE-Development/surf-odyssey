package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.RegionDsl
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * One possible outcome when a mob of a mob region dies.
 *
 * @property weight relative chance of this outcome, positive
 * @property item creates the dropped item, or `null` if this outcome drops nothing
 */
class MobLootEntry(val weight: Int, val item: (() -> ItemStack)?) {
    init {
        require(weight > 0) { "Mob loot entries need a positive weight, got $weight" }
    }
}

/**
 * The code loot of the mobs of a mob region: on every death one entry is picked by weight.
 *
 * @property entries the possible outcomes
 */
class MobLootTable(val entries: List<MobLootEntry>) {
    /** The sum of all entry weights. */
    private val totalWeight = entries.sumOf { it.weight }

    /**
     * Picks an entry by weight.
     *
     * @param random the source of randomness
     * @return the picked entry, or `null` if the table is empty
     */
    fun roll(random: Random): MobLootEntry? {
        if (entries.isEmpty()) return null
        var pick = random.nextInt(totalWeight)
        for (entry in entries) {
            pick -= entry.weight
            if (pick < 0) return entry
        }
        return entries.last()
    }

    /**
     * Picks an entry by weight and creates its item.
     *
     * @param random the source of randomness
     * @return the dropped item, or nothing if the table is empty or the picked entry drops nothing
     */
    fun drops(random: Random): List<ItemStack> = listOfNotNull(roll(random)?.item?.invoke())

    /** Holds the table without loot. */
    companion object {
        /** A table that never drops anything. */
        val EMPTY = MobLootTable(emptyList())
    }
}

/** Collects the entries of a [MobLootTable] in declaration order. */
@RegionDsl
class MobLootBuilder {
    /** The collected entries. */
    private val entries = mutableListOf<MobLootEntry>()

    /**
     * Adds an outcome with [weight] that drops the item [item] creates.
     *
     * @param weight relative chance, positive
     * @param item creates the dropped item
     */
    fun entry(weight: Int, item: () -> ItemStack) {
        entries += MobLootEntry(weight, item)
    }

    /**
     * Adds an outcome with [weight] that drops nothing.
     *
     * @param weight relative chance, positive
     */
    fun nothing(weight: Int) {
        entries += MobLootEntry(weight, null)
    }

    /** Creates the table from the collected entries. */
    fun build(): MobLootTable = MobLootTable(entries.toList())
}
