package dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShapeTest {
    private val lShape = Prism(listOf(Point2(0, 0), Point2(10, 0), Point2(10, 4), Point2(4, 4), Point2(4, 10), Point2(0, 10)), minY = 60, maxY = 70)

    @Test
    fun `a cuboid contains its corners regardless of corner order`() {
        val cuboid = Cuboid.of(BlockPoint(5, 70, 5), BlockPoint(0, 60, 0))
        assertTrue(cuboid.contains(0, 60, 0))
        assertTrue(cuboid.contains(5, 70, 5))
        assertFalse(cuboid.contains(6, 65, 3))
    }

    @Test
    fun `a prism contains blocks inside, on edges and on corners`() {
        assertTrue(lShape.contains(2, 65, 2))
        assertTrue(lShape.contains(10, 60, 2))
        assertTrue(lShape.contains(4, 70, 4))
        assertTrue(lShape.contains(0, 65, 10))
    }

    @Test
    fun `a prism excludes the notch of the L and blocks outside its height`() {
        assertFalse(lShape.contains(7, 65, 7))
        assertFalse(lShape.contains(2, 59, 2))
        assertFalse(lShape.contains(2, 71, 2))
    }

    @Test
    fun `a compound shape adds includes and removes excludes`() {
        val forest = shape {
            prism(minY = 60, maxY = 90) { point(0, 0); point(20, 0); point(20, 20); point(0, 20) }
            exclude { cuboid(5, 60, 5, 9, 70, 9) }
        }
        assertTrue(forest.contains(1, 65, 1))
        assertFalse(forest.contains(7, 65, 7))
        assertTrue(forest.contains(7, 75, 7))
    }

    @Test
    fun `bounds of a compound shape span all includes`() {
        val s = shape { cuboid(0, 0, 0, 1, 1, 1); cuboid(10, 5, 10, 12, 6, 12) }
        assertTrue(s.bounds == Bounds(0, 0, 0, 12, 6, 12))
    }

    @Test
    fun `invalid shapes are rejected`() {
        assertFailsWith<IllegalArgumentException> { Prism(listOf(Point2(0, 0), Point2(1, 1)), 0, 1) }
        assertFailsWith<IllegalArgumentException> { Prism(listOf(Point2(0, 0), Point2(1, 0), Point2(1, 1)), 5, 1) }
        assertFailsWith<IllegalArgumentException> { shape { } }
    }
}
