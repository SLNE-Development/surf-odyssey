package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WeightedTableTest {
    @Test
    fun `picks entries roughly by weight`() {
        val table = WeightedTable(mapOf("common" to 9, "rare" to 1))
        val random = Random(42)
        val counts = List(10_000) { table.pick(random) }.groupingBy { it }.eachCount()
        val rare = counts["rare"] ?: 0
        assertTrue(rare in 800..1200, "rare picked $rare times")
    }

    @Test
    fun `a single entry is always picked`() {
        assertEquals("only", WeightedTable(mapOf("only" to 3)).pick(Random(1)))
    }

    @Test
    fun `empty or non positive weights are rejected`() {
        assertFailsWith<IllegalArgumentException> { WeightedTable(emptyMap<String, Int>()) }
        assertFailsWith<IllegalArgumentException> { WeightedTable(mapOf("x" to 0)) }
    }

    @Test
    fun `weights that sum past the int range still work`() {
        val table = WeightedTable(mapOf("a" to Int.MAX_VALUE, "b" to Int.MAX_VALUE))
        val picks = List(200) { table.pick(Random(it)) }.toSet()
        assertEquals(setOf("a", "b"), picks)
    }
}
