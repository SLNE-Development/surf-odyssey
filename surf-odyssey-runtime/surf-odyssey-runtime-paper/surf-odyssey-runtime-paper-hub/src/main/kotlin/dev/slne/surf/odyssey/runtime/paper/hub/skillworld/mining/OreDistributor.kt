package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining

import kotlin.random.Random

object OreDistributor {
    fun <B> distribute(blocks: List<B>, chance: Double, tables: Map<B, WeightedTable<B>>, random: Random): Map<Int, B> {
        val ores = HashMap<Int, B>()
        blocks.forEachIndexed { index, block ->
            val table = tables[block] ?: return@forEachIndexed
            if (random.nextDouble() < chance) ores[index] = table.pick(random)
        }
        return ores
    }
}
