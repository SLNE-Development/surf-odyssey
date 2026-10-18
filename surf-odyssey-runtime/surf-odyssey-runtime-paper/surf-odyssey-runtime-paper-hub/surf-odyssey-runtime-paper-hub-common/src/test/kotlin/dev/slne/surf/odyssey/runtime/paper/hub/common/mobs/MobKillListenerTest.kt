package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import org.bukkit.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests which kills count for the killer's quests and weapon. */
class MobKillListenerTest {
    private val wolves = RegionRegistry().apply {
        mobSpawn("wolves") {
            shape { cuboid(0, 0, 0, 10, 10, 10) }
            mobs(EntityType.WOLF)
            spawnPoint(5, 5, 5)
        }
    }.build().single()

    @Test
    fun `a kill outside every region counts`() {
        assertTrue(MobKillListener.counts(null) { true })
    }

    @Test
    fun `a kill in a region that admits the killer counts`() {
        assertTrue(MobKillListener.counts(wolves) { false })
    }

    @Test
    fun `a kill in a region the killer is locked out of does not count`() {
        assertFalse(MobKillListener.counts(wolves) { it === wolves })
    }
}
