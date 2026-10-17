package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OreDistributorTest {
    private val tables = mapOf(
        "stone" to WeightedTable(mapOf("coal" to 1)),
        "deepslate" to WeightedTable(mapOf("deep_coal" to 1)),
    )

    @Test
    fun `only carrier blocks become ores`() {
        val blocks = listOf("stone", "air", "deepslate", "dirt")
        val ores = OreDistributor.distribute(blocks, 1.0, tables, Random(1))
        assertEquals(mapOf(0 to "coal", 2 to "deep_coal"), ores)
    }

    @Test
    fun `chance controls how many carriers turn into ores`() {
        val blocks = List(20_000) { "stone" }
        val ores = OreDistributor.distribute(blocks, 0.05, tables, Random(7))
        assertTrue(ores.size in 800..1200, "got ${ores.size} ores")
    }

    @Test
    fun `different seeds place ores differently`() {
        val blocks = List(1_000) { "stone" }
        val first = OreDistributor.distribute(blocks, 0.05, tables, Random(1)).keys
        val second = OreDistributor.distribute(blocks, 0.05, tables, Random(2)).keys
        assertTrue(first != second)
    }

    @Test
    fun `zero chance places nothing`() {
        assertTrue(OreDistributor.distribute(List(100) { "stone" }, 0.0, tables, Random(1)).isEmpty())
    }
}
