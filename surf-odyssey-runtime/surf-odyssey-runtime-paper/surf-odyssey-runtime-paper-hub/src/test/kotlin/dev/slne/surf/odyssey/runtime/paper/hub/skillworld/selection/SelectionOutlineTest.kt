package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.selection

import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockPos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectionOutlineTest {
    private fun corners(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) =
        listOf(minX, maxX).flatMap { x -> listOf(minY, maxY).flatMap { y -> listOf(minZ, maxZ).map { z -> OutlinePoint(x, y, z) } } }

    @Test
    fun `single block yields its eight corners`() {
        val points = selectionOutline(BlockPos(4, 5, 6), BlockPos(4, 5, 6), 400)

        assertEquals(corners(4.0, 5.0, 6.0, 5.0, 6.0, 7.0).toSet(), points.toSet())
        assertEquals(8, points.size)
    }

    @Test
    fun `three by two by one box samples every block edge`() {
        val points = selectionOutline(BlockPos(2, 1, 0), BlockPos(0, 0, 0), 400)

        assertEquals(20, points.size)
        assertTrue(OutlinePoint(1.0, 0.0, 0.0) in points)
        assertTrue(OutlinePoint(3.0, 1.0, 0.0) in points)
        assertTrue(OutlinePoint(0.0, 1.0, 1.0) in points)
    }

    @Test
    fun `corners of the full block span are always included`() {
        val points = selectionOutline(BlockPos(-3, 70, 9), BlockPos(1000, 10, -500), 50).toSet()

        assertTrue(points.containsAll(corners(-3.0, 10.0, -500.0, 1001.0, 71.0, 10.0)))
    }

    @Test
    fun `huge box respects the point cap`() {
        val points = selectionOutline(BlockPos(0, 0, 0), BlockPos(10_000, 10_000, 10_000), 400)

        assertTrue(points.size <= 400)
        assertTrue(points.size > 8)
    }

    @Test
    fun `result contains no duplicates`() {
        val points = selectionOutline(BlockPos(0, 0, 0), BlockPos(1, 0, 7), 400)

        assertEquals(points.size, points.toSet().size)
    }

    @Test
    fun `cap below eight returns the corners`() {
        val points = selectionOutline(BlockPos(0, 0, 0), BlockPos(5, 5, 5), 3)

        assertEquals(corners(0.0, 0.0, 0.0, 6.0, 6.0, 6.0).toSet(), points.toSet())
        assertEquals(8, points.size)
    }

    @Test
    fun `flat box under a tight cap uses a coarse step without duplicates`() {
        val points = selectionOutline(BlockPos(0, 0, 0), BlockPos(99, 0, 99), 20)

        assertTrue(points.size <= 20)
        assertEquals(points.size, points.toSet().size)
        assertTrue(points.toSet().containsAll(corners(0.0, 0.0, 0.0, 100.0, 1.0, 100.0)))
        assertTrue(points.size > 8)
    }
}
