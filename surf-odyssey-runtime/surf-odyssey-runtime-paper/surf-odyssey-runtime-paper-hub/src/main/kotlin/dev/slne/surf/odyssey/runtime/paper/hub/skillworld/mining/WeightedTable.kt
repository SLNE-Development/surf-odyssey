package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining

import kotlin.random.Random

class WeightedTable<T>(weights: Map<T, Int>) {
    private val entries = weights.entries.toList()
    private val total = entries.sumOf { it.value.toLong() }

    init {
        require(entries.isNotEmpty()) { "A weighted table needs at least one entry" }
        require(entries.all { it.value > 0 }) { "Weights must be positive" }
    }

    fun pick(random: Random): T {
        var roll = random.nextLong(total)
        for ((value, weight) in entries) {
            if (roll < weight) return value
            roll -= weight
        }
        return entries.last().key
    }
}
