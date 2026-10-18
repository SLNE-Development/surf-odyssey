package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import org.bukkit.Material
import org.bukkit.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Tests which mob region's code loot a dying mob drops. */
class MobRegionLootTest {
    private val regions = RegionRegistry().apply {
        mobSpawn("wolves") {
            shape { cuboid(0, 0, 0, 10, 10, 10) }
            mobs(EntityType.WOLF)
            spawnPoint(5, 5, 5)
        }
        mobSpawn("bears") {
            shape { cuboid(20, 0, 0, 30, 10, 10) }
            mobs(EntityType.POLAR_BEAR)
            spawnPoint(25, 5, 5)
        }
        mine("mine") {
            shape { cuboid(40, 0, 0, 50, 10, 10) }
            breakable(Material.COAL_ORE)
        }
    }.build()

    private val wolves = regions[0] as MobSpawnRegion
    private val bears = regions[1] as MobSpawnRegion

    /** Returns the region that covers [point] among [regions]. */
    private fun at(point: BlockPoint) = regions.firstOrNull { it.shape.contains(point.x, point.y, point.z) }

    @Test
    fun `a mob drops the loot of the region that spawned it, wherever it dies`() =
        assertSame(wolves, MobRegionLoot.regionOf(regions, { it === wolves }, at(BlockPoint(25, 5, 5))))

    @Test
    fun `a mob no region spawned drops the loot of the mob region it dies in`() =
        assertSame(bears, MobRegionLoot.regionOf(regions, { false }, at(BlockPoint(25, 5, 5))))

    @Test
    fun `a mob dying in another kind of region drops nothing`() =
        assertNull(MobRegionLoot.regionOf(regions, { false }, at(BlockPoint(45, 5, 5))))

    @Test
    fun `a mob dying outside every region drops nothing`() =
        assertNull(MobRegionLoot.regionOf(regions, { false }, at(BlockPoint(100, 5, 5))))

    @Test
    fun `a mob without a player killer drops nothing`() =
        assertNull(MobRegionLoot.lootRegion(regions, { it === wolves }, at(BlockPoint(5, 5, 5)), admits = null))

    @Test
    fun `a killer the region locks out gets no loot`() =
        assertNull(MobRegionLoot.lootRegion(regions, { it === wolves }, at(BlockPoint(5, 5, 5)), admits = { false }))

    @Test
    fun `a killer the region admits gets its loot`() =
        assertSame(wolves, MobRegionLoot.lootRegion(regions, { it === wolves }, at(BlockPoint(5, 5, 5)), admits = { it === wolves }))
}
