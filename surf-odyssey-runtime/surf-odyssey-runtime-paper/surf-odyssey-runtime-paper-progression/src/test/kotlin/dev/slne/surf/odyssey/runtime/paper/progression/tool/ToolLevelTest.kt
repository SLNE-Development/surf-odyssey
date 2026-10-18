package dev.slne.surf.odyssey.runtime.paper.progression.tool

import dev.slne.surf.odyssey.runtime.paper.progression.definition.curve.Curves
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.tool
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests for the level of a tool and the detection of level-ups. */
class ToolLevelTest {
    /** A rod on a quadratic curve that needs 4 938 usage for level 3. */
    private val rod = tool("rod", ToolType.ROD) {
        name = text("Angel")
        material = Material.FISHING_ROD
        curve = Curves.quadratic(100_000)
    }

    @Test
    fun `usage of 4938 on a quadratic curve is level 3`() {
        assertEquals(3, ToolProgress.levelFor(rod, 4_938))
    }

    @Test
    fun `usage just below a threshold keeps the lower level`() {
        assertEquals(2, ToolProgress.levelFor(rod, 4_937))
    }

    @Test
    fun `adding 50 to 4900 is a level-up`() {
        assertTrue(ToolProgress.isLevelUp(rod, before = 4_900, added = 50))
    }

    @Test
    fun `adding usage within a level is no level-up`() {
        assertFalse(ToolProgress.isLevelUp(rod, before = 4_950, added = 10))
    }

    @Test
    fun `usage beyond the maximum level is no level-up`() {
        assertEquals(10, ToolProgress.levelFor(rod, 200_000))
        assertFalse(ToolProgress.isLevelUp(rod, before = 200_000, added = 1_000))
    }

    @Test
    fun `usage needed for the next level is absent at the maximum level`() {
        assertEquals(4_938, ToolProgress.usageForNextLevel(rod, 2))
        assertNull(ToolProgress.usageForNextLevel(rod, 10))
    }
}
