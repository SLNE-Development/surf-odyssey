package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FishTableTest {
    private val dry = FishContext(raining = false, thundering = false, night = false, biome = "minecraft:plains", waterDepth = 3)
    private fun entry(id: String, weight: Int, vararg conditions: FishCondition) =
        FishEntry(id, weight, conditions.toList()) { error("not needed") }

    @Test
    fun `only fish whose conditions hold can be caught`() {
        val table = FishTable(listOf(entry("trout", 10, FishCondition.rain()), entry("perch", 10)))
        repeat(50) { assertEquals("perch", table.roll(dry, Random(it))?.id) }
    }

    @Test
    fun `weights decide how often a fish is caught`() {
        val table = FishTable(listOf(entry("common", 90), entry("rare", 10)))
        val rare = (0 until 10_000).count { table.roll(dry, Random(it))?.id == "rare" }
        assertTrue(rare in 800..1200, "rare caught $rare times")
    }

    @Test
    fun `nothing is caught when no fish fits`() =
        assertNull(FishTable(listOf(entry("deep", 1, FishCondition.minDepth(10)))).roll(dry, Random(1)))

    @Test
    fun `conditions combine`() {
        val ctx = dry.copy(raining = true, night = true, biome = "minecraft:river")
        assertEquals(true, FishCondition.biome("minecraft:river").test(ctx))
        assertEquals(false, FishCondition.day().test(ctx))
        assertEquals(true, FishCondition.night().test(ctx))
    }
}
