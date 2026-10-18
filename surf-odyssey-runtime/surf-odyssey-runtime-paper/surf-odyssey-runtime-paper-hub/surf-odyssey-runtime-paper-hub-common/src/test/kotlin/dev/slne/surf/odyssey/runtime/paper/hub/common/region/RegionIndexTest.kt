package dev.slne.surf.odyssey.runtime.paper.hub.common.region

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Checks how [RegionIndex] resolves blocks to regions, what the declared regions allow and which regions are rejected. */
class RegionIndexTest {
    /** Returns a mine and a forest that do not overlap. */
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
    fun `the mob spawn regions are listed apart`() {
        val regions = registry() + RegionRegistry().apply {
            mobSpawn("north_wolves") {
                shape { cuboid(40, 0, 40, 49, 9, 49) }
                mobs(EntityType.WOLF)
                spawnPoint(45, 5, 45)
            }
        }.build()
        assertEquals(listOf("north_wolves"), RegionIndex(regions).mobSpawnRegions.map { it.id })
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
            mine("upper_mine") { shape { cuboid(0, 0, 0, 5, 5, 5) } }
            mine("lower_mine") { shape { cuboid(5, 5, 5, 8, 8, 8) } }
        }.build()
        val error = assertFailsWith<IllegalStateException> { RegionIndex(regions) }
        val message = error.message.orEmpty()
        assertTrue("upper_mine" in message, message)
        assertTrue("lower_mine" in message, message)
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

    @Test
    fun `overlapping prisms are rejected with both ids`() {
        val regions = RegionRegistry().apply {
            mine("prism_a") { shape { prism(0, 10) { point(0, 0); point(10, 0); point(10, 10); point(0, 10) } } }
            mine("prism_b") { shape { prism(5, 15) { point(8, 8); point(20, 8); point(20, 20); point(8, 20) } } }
        }.build()
        val error = assertFailsWith<IllegalStateException> { RegionIndex(regions) }
        assertTrue("prism_a" in error.message!! && "prism_b" in error.message!!)
    }

    @Test
    fun `only the overlapping pair of three regions is reported`() {
        val regions = RegionRegistry().apply {
            mine("alpha") { shape { cuboid(0, 0, 0, 5, 5, 5) } }
            mine("beta") { shape { cuboid(5, 0, 0, 9, 5, 5) } }
            mine("gamma") { shape { cuboid(50, 0, 50, 55, 5, 55) } }
        }.build()
        assertEquals(listOf("alpha" to "beta"), RegionIndex.overlaps(regions))
        val error = assertFailsWith<IllegalStateException> { RegionIndex(regions) }
        assertTrue("alpha" in error.message!! && "beta" in error.message!!)
        assertFalse("gamma" in error.message!!)
    }
}
