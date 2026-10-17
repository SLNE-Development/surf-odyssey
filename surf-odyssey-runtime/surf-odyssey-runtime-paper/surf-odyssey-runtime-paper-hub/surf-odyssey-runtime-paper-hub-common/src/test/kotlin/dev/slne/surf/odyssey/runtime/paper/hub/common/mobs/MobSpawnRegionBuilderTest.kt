package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import org.bukkit.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Checks the settings [MobSpawnRegionBuilder] accepts and rejects. */
class MobSpawnRegionBuilderTest {
    /** Builds a mob spawn region whose defaults are valid and changed by [block]. */
    private fun registry(withShape: Boolean = true, block: MobSpawnRegionBuilder.() -> Unit = {}) = RegionRegistry().apply {
        mobSpawn("north_wolves") {
            if (withShape) shape { cuboid(0, 0, 0, 10, 10, 10) }
            block()
        }
    }

    /** Builds a region with a wolf at a spawn point at [x], changed by [block]. */
    private fun wolves(x: Int = 5, block: MobSpawnRegionBuilder.() -> Unit = {}) = registry {
        mobs(EntityType.WOLF)
        spawnPoint(x, 5, 5)
        block()
    }

    /** Asserts that building with [block] fails with a message naming the region. */
    private fun assertRejected(block: () -> Unit) {
        val error = assertFailsWith<IllegalArgumentException> { block() }
        assertTrue("north_wolves" in error.message.orEmpty(), error.message)
    }

    @Test
    fun `a spawn point inside the shape is accepted`() = assertEquals(1, wolves().build().size)

    @Test
    fun `a spawn point outside the shape fails with the region id`() = assertRejected { wolves(x = 50) }

    @Test
    fun `a region without mob types fails`() = assertRejected { registry { spawnPoint(5, 5, 5) } }

    @Test
    fun `a region without spawn points fails`() = assertRejected { registry { mobs(EntityType.WOLF) } }

    @Test
    fun `a region without a shape fails`() = assertRejected {
        registry(withShape = false) {
            mobs(EntityType.WOLF)
            spawnPoint(5, 5, 5)
        }
    }

    @Test
    fun `a non-positive maximum fails`() {
        assertRejected { wolves { maxAlive = 0 } }
        assertRejected { wolves { maxAlive = -1 } }
    }

    @Test
    fun `a non-positive respawn interval fails`() {
        assertRejected { wolves { respawnInterval = Duration.ZERO } }
        assertRejected { wolves { respawnInterval = (-1).seconds } }
    }

    @Test
    fun `types that are no spawnable mobs fail`() {
        assertRejected { wolves { mobs(EntityType.ARMOR_STAND) } }
        assertRejected { wolves { mobs(EntityType.PLAYER) } }
    }
}
