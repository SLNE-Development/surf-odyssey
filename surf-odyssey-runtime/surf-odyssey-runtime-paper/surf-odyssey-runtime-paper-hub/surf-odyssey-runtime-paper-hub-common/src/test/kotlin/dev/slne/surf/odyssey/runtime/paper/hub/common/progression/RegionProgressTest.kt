package dev.slne.surf.odyssey.runtime.paper.hub.common.progression

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.ForestRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.MineRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.shape
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import net.kyori.adventure.text.Component
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

/** Tests which tool a region action levels and how much a catch is worth. */
class RegionProgressTest {
    private val area = shape { cuboid(0, 0, 0, 1, 1, 1) }

    @Test
    fun `mines level pickaxes and forests level axes`() {
        val mine = MineRegion("mine", Component.text("mine"), area, setOf(Material.COAL_ORE), Material.STONE, 1.minutes, null)
        val forest = ForestRegion("forest", Component.text("forest"), area, setOf(Material.OAK_LOG), 1.minutes, null)
        assertEquals(ToolType.PICKAXE, RegionProgress.harvestTool(mine))
        assertEquals(ToolType.AXE, RegionProgress.harvestTool(forest))
    }

    @Test
    fun `common fish are worth one use and rare fish more`() {
        assertEquals(1, RegionProgress.catchUsage(70))
        assertEquals(1, RegionProgress.catchUsage(30))
        assertEquals(2, RegionProgress.catchUsage(20))
        assertEquals(3, RegionProgress.catchUsage(10))
        assertEquals(3, RegionProgress.catchUsage(1))
    }
}
