package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal

import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.PortalKind
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockBox
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockPos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PortalBlocksTest {
    private fun portal(id: String, box: BlockBox) = Portal(id, PortalKind.ENTRY, SkillType.MINING, box)

    @Test
    fun `body cells are the feet and the head block`() {
        assertEquals(listOf(BlockPos(3, 64, -2), BlockPos(3, 65, -2)), PortalBlocks.bodyCells(3, 64, -2))
    }

    @Test
    fun `counting ignores holes in the box`() {
        val box = BlockBox(0, 0, 0, 2, 0, 0)
        val result = PortalBlocks.countPortalBlocks(box, { _, _ -> true }) { x, _, _ -> x != 1 }
        assertEquals(PortalBlocks.ScanResult(2, 0), result)
    }

    @Test
    fun `counting finds a diagonal staircase`() {
        val box = BlockBox(0, 0, 0, 3, 3, 0)
        val result = PortalBlocks.countPortalBlocks(box, { _, _ -> true }) { x, y, _ -> x == y }
        assertEquals(PortalBlocks.ScanResult(4, 0), result)
    }

    @Test
    fun `counting without a match returns zero`() {
        val result = PortalBlocks.countPortalBlocks(BlockBox(0, 0, 0, 4, 4, 4), { _, _ -> true }) { _, _, _ -> false }
        assertEquals(PortalBlocks.ScanResult(0, 0), result)
    }

    @Test
    fun `counting skips unloaded chunks and reports each once`() {
        val box = BlockBox(0, 0, 0, 47, 1, 15)
        val asked = mutableListOf<Pair<Int, Int>>()
        val result = PortalBlocks.countPortalBlocks(box, { cx, cz -> asked += cx to cz; cx != 1 }) { _, _, _ -> true }
        assertEquals(PortalBlocks.ScanResult(2 * 16 * 16 * 2, 1), result)
        assertEquals(3, asked.size)
    }

    @Test
    fun `counting handles negative coordinates across chunk borders`() {
        val box = BlockBox(-1, 0, -1, 0, 0, 0)
        val result = PortalBlocks.countPortalBlocks(box, { cx, cz -> cx == 0 && cz == 0 }) { _, _, _ -> true }
        assertEquals(PortalBlocks.ScanResult(1, 3), result)
    }

    @Test
    fun `scan limit is exceeded only above the limit`() {
        val atLimit = BlockBox(0, 0, 0, 99, 99, 99)
        val above = BlockBox(0, 0, 0, 100, 99, 99)
        assertFalse(PortalBlocks.exceedsScanLimit(atLimit))
        assertTrue(PortalBlocks.exceedsScanLimit(above))
        assertTrue(PortalBlocks.exceedsScanLimit(atLimit, 999_999))
    }

    @Test
    fun `trigger picks the portal whose box holds a portal block cell`() {
        val first = portal("first", BlockBox(0, 0, 0, 1, 1, 1))
        val second = portal("second", BlockBox(10, 0, 0, 11, 1, 1))
        val cells = listOf(BlockPos(10, 0, 0), BlockPos(10, 1, 0))
        assertSame(second, PortalBlocks.triggers(listOf(first, second), cells) { _, _, _ -> true })
    }

    @Test
    fun `trigger uses the head cell when the feet cell is no portal block`() {
        val portal = portal("p", BlockBox(0, 0, 0, 0, 1, 0))
        val cells = PortalBlocks.bodyCells(0, 0, 0)
        assertSame(portal, PortalBlocks.triggers(listOf(portal), cells) { _, y, _ -> y == 1 })
    }

    @Test
    fun `trigger is null when the cells lie outside every box`() {
        val portal = portal("p", BlockBox(0, 0, 0, 1, 1, 1))
        assertNull(PortalBlocks.triggers(listOf(portal), listOf(BlockPos(5, 5, 5))) { _, _, _ -> true })
    }

    @Test
    fun `trigger does not look up blocks outside every box`() {
        val portal = portal("p", BlockBox(0, 0, 0, 1, 1, 1))
        val cells = listOf(BlockPos(5, 5, 5), BlockPos(0, 0, 0))
        val found = PortalBlocks.triggers(listOf(portal), cells) { x, y, z ->
            assertTrue(x == 0 && y == 0 && z == 0, "looked up outside the box")
            true
        }
        assertSame(portal, found)
    }

    @Test
    fun `trigger is null when the cell inside the box is no portal block`() {
        val portal = portal("p", BlockBox(0, 0, 0, 1, 1, 1))
        assertNull(PortalBlocks.triggers(listOf(portal), listOf(BlockPos(0, 0, 0))) { _, _, _ -> false })
    }
}
