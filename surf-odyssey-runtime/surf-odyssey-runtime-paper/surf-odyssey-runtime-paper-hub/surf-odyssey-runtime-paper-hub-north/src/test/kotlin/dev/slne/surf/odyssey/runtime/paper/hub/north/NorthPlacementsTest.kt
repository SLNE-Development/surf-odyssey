package dev.slne.surf.odyssey.runtime.paper.hub.north

import dev.slne.surf.odyssey.runtime.paper.hub.common.mobs.MobSpawnRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionIndex
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Bounds
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionPlacements
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestStepRequirement
import kotlin.math.floor
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Tests where North places its NPCs, which region it gates and what its wolves drop. */
class NorthPlacementsTest {
    /** The regions of North. */
    private val regions = RegionIndex(RegionRegistry().apply { northRegions() }.build())

    /** The placements of North. */
    private val placements = ProgressionPlacements().apply { northPlacements() }

    @Test
    fun `olek stands at the lake and mira at the ruins`() {
        assertEquals(listOf("olek", "mira"), placements.npcs.map { it.npc.id })
        assertNear("olek", "north_lake")
        assertNear("mira", "north_ruins")
        assertTrue(placements.stations.isEmpty())
    }

    @Test
    fun `npcs stand on the ground outside every region and portal`() {
        placements.npcs.forEach { placement ->
            val (x, y, z) = blockOf(placement.npc.id)
            assertEquals(64, y, "${placement.npc.id} stands on the grass")
            assertNull(regions.regionAt(x, y, z), "${placement.npc.id} stands in a region")
            northPortals().forEach { assertFalse(it.shape.contains(x, y, z), "${placement.npc.id} stands in ${it.id}") }
        }
    }

    @Test
    fun `the ridge mine opens at the third step of the lighthouse questline`() {
        assertEquals(QuestStepRequirement("leuchtfeuer", 2), regions.byId("north_mine_ridge")?.gate)
    }

    @Test
    fun `wolves drop bones leather or nothing`() {
        val wolves = regions.byId("north_wolves") as MobSpawnRegion
        assertEquals(listOf(60, 30, 10), wolves.loot.entries.map { it.weight })
        assertNotNull(wolves.loot.entries[0].item)
        assertNotNull(wolves.loot.entries[1].item)
        assertNull(wolves.loot.entries[2].item)
    }

    /** Returns the block the NPC [id] stands in. */
    private fun blockOf(id: String): Triple<Int, Int, Int> {
        val at = placements.npcs.single { it.npc.id == id }.position
        return Triple(floor(at.x).toInt(), floor(at.y).toInt(), floor(at.z).toInt())
    }

    /** Asserts that the NPC [id] stands at most 6 blocks beside the region [regionId]. */
    private fun assertNear(id: String, regionId: String) {
        val (x, _, z) = blockOf(id)
        val bounds: Bounds = assertNotNull(regions.byId(regionId)).shape.bounds
        val dx = max(0, max(bounds.minX - x, x - bounds.maxX))
        val dz = max(0, max(bounds.minZ - z, z - bounds.maxZ))
        assertTrue(dx <= 6 && dz <= 6, "$id stands $dx/$dz blocks from $regionId")
    }
}
