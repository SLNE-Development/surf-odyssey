package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import org.bukkit.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MobSpawnRegionBuilderTest {
    private fun registry(x: Int) = RegionRegistry().apply {
        mobSpawn("north_wolves") {
            shape { cuboid(0, 0, 0, 10, 10, 10) }
            mobs(EntityType.WOLF)
            spawnPoint(x, 5, 5)
        }
    }

    @Test
    fun `a spawn point inside the shape is accepted`() = assertEquals(1, registry(5).build().size)

    @Test
    fun `a spawn point outside the shape fails with the region id`() {
        val error = assertFailsWith<IllegalArgumentException> { registry(50) }
        assertTrue("north_wolves" in error.message.orEmpty())
    }
}
