package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import org.bukkit.inventory.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Checks the settings [FishingRegionBuilder] requires. */
class FishingRegionBuilderTest {
    @Test
    fun `a fishing region with a fish is accepted`() {
        val region = RegionRegistry().apply {
            fishing("north_lake") {
                shape { cuboid(0, 0, 0, 1, 1, 1) }
                fish("perch", 1) { item { ItemStack.empty() } }
            }
        }.build().single() as FishingRegion
        assertEquals(listOf("perch"), region.table.entries.map { it.id })
    }

    @Test
    fun `a fishing region without fish fails with the region id`() {
        val error = assertFailsWith<IllegalArgumentException> {
            RegionRegistry().apply { fishing("north_lake") { shape { cuboid(0, 0, 0, 1, 1, 1) } } }
        }
        assertTrue("north_lake" in error.message.orEmpty())
    }
}
