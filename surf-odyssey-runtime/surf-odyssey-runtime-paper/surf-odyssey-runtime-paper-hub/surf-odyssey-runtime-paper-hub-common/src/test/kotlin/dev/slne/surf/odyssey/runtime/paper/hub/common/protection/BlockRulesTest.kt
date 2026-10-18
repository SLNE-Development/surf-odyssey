package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks how [BlockRules] decide about breaking a block. */
class BlockRulesTest {
    private val mine = RegionRegistry().apply {
        mine("m") { shape { cuboid(0, 0, 0, 1, 1, 1) }; breakable(Material.IRON_ORE) }
    }.build().single()

    @Test
    fun `outside regions nothing can be broken`() =
        assertEquals(BreakDecision.DENY, BlockRules.decide(null, Material.STONE, pending = false))

    @Test
    fun `listed blocks are harvested, others denied`() {
        assertEquals(BreakDecision.HARVEST, BlockRules.decide(mine, Material.IRON_ORE, pending = false))
        assertEquals(BreakDecision.DENY, BlockRules.decide(mine, Material.STONE, pending = false))
    }

    @Test
    fun `a block waiting for regeneration cannot be broken again`() =
        assertEquals(BreakDecision.DENY, BlockRules.decide(mine, Material.IRON_ORE, pending = true))

    @Test
    fun `physics may destroy only blocks the region lets players break`() {
        assertTrue(BlockRules.allowsDestroy(mine, Material.IRON_ORE, pending = false))
        assertFalse(BlockRules.allowsDestroy(mine, Material.TORCH, pending = false))
        assertFalse(BlockRules.allowsDestroy(null, Material.LADDER, pending = false))
        assertFalse(BlockRules.allowsDestroy(mine, Material.IRON_ORE, pending = true))
    }
}
