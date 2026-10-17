package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockRulesTest {
    private val mine = RegionRegistry().apply {
        mine("m") { shape { cuboid(0, 0, 0, 1, 1, 1) }; breakable(Material.IRON_ORE) }
    }.build().single()

    @Test
    fun `outside regions nothing can be broken`() =
        assertEquals(BreakDecision.DENY, BlockRules.decide(null, Material.STONE, pending = false, building = false))

    @Test
    fun `listed blocks are harvested, others denied`() {
        assertEquals(BreakDecision.HARVEST, BlockRules.decide(mine, Material.IRON_ORE, pending = false, building = false))
        assertEquals(BreakDecision.DENY, BlockRules.decide(mine, Material.STONE, pending = false, building = false))
    }

    @Test
    fun `a block waiting for regeneration cannot be broken again`() =
        assertEquals(BreakDecision.DENY, BlockRules.decide(mine, Material.IRON_ORE, pending = true, building = false))

    @Test
    fun `build mode allows everything without regeneration`() =
        assertEquals(BreakDecision.ALLOW, BlockRules.decide(null, Material.STONE, pending = false, building = true))
}
