package dev.slne.surf.odyssey.runtime.paper.hub.common.region

import net.kyori.adventure.text.Component
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegionIndexTest {
    private fun registry() = RegionRegistry().apply {
        mine("north_mine") {
            displayName = Component.text("Grat")
            shape { cuboid(0, 0, 0, 9, 9, 9) }
            breakable(Material.IRON_ORE)
        }
        forest("north_forest") {
            shape { cuboid(20, 0, 20, 29, 9, 29) }
            breakable(Material.OAK_LOG, Material.OAK_LEAVES)
        }
    }.build()

    @Test
    fun `blocks resolve to the region that covers them`() {
        val index = RegionIndex(registry())
        assertEquals("north_mine", index.regionAt(5, 5, 5)?.id)
        assertEquals("north_forest", index.regionAt(25, 5, 25)?.id)
        assertNull(index.regionAt(15, 5, 15))
    }

    @Test
    fun `harvest regions only allow their listed blocks`() {
        val mine = RegionIndex(registry()).byId("north_mine")!!
        assertTrue(mine.canBreak(Material.IRON_ORE))
        assertFalse(mine.canBreak(Material.STONE))
    }

    @Test
    fun `mines fall back to stone as placeholder and forests to air`() {
        val index = RegionIndex(registry())
        assertEquals(Material.STONE, (index.byId("north_mine") as MineRegion).placeholder)
        assertEquals(Material.AIR, (index.byId("north_forest") as ForestRegion).placeholder)
    }

    @Test
    fun `overlapping regions are rejected with both ids`() {
        val regions = RegionRegistry().apply {
            mine("a") { shape { cuboid(0, 0, 0, 5, 5, 5) } }
            mine("b") { shape { cuboid(5, 5, 5, 8, 8, 8) } }
        }.build()
        val error = assertFailsWith<IllegalStateException> { RegionIndex(regions) }
        assertTrue("a" in error.message!! && "b" in error.message!!)
    }

    @Test
    fun `regions whose bounds touch but whose shapes do not are accepted`() {
        val regions = RegionRegistry().apply {
            forest("canopy") {
                shape {
                    cuboid(0, 60, 0, 10, 80, 10)
                    exclude { cuboid(3, 60, 3, 6, 66, 6) }
                }
            }
            mine("cellar") { shape { cuboid(3, 60, 3, 6, 66, 6) } }
        }.build()
        RegionIndex(regions)
    }

    @Test
    fun `duplicate and malformed ids are rejected`() {
        assertFailsWith<IllegalStateException> {
            RegionIndex(RegionRegistry().apply {
                mine("x") { shape { cuboid(0, 0, 0, 1, 1, 1) } }
                mine("x") { shape { cuboid(5, 5, 5, 6, 6, 6) } }
            }.build())
        }
        assertFailsWith<IllegalArgumentException> {
            RegionRegistry().apply { mine("Bad Id") { shape { cuboid(0, 0, 0, 1, 1, 1) } } }.build()
        }
    }
}
