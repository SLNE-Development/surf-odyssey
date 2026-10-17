package dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    @Test
    fun `a point on a horizontal edge that is not a vertex is inside`() {
        assertTrue(lShape.contains(2, 65, 0))
        assertTrue(lShape.contains(7, 65, 4))
    }

    @Test
    fun `points on the reflex notch edges are inside and points in the notch are outside`() {
        assertTrue(lShape.contains(4, 65, 7))
        assertTrue(lShape.contains(7, 65, 4))
        assertTrue(lShape.contains(4, 65, 4))
        assertFalse(lShape.contains(5, 65, 5))
        assertFalse(lShape.contains(5, 65, 7))
        assertFalse(lShape.contains(7, 65, 5))
    }

    @Test
    fun `a prism spanning negative coordinates contains its edges and vertices`() {
        val square = Prism(listOf(Point2(-5, -5), Point2(5, -5), Point2(5, 5), Point2(-5, 5)), minY = -10, maxY = 10)
        assertTrue(square.contains(-5, 0, -5))
        assertTrue(square.contains(5, 0, 5))
        assertTrue(square.contains(0, 0, -5))
        assertTrue(square.contains(-5, 0, 3))
        assertTrue(square.contains(0, 0, 0))
        assertFalse(square.contains(-6, 0, 0))
        assertFalse(square.contains(0, 0, 6))
        assertFalse(square.contains(6, 0, -6))
    }

    @Test
    fun `minY and maxY are inclusive and the blocks beyond them are outside`() {
        assertTrue(lShape.contains(2, 60, 2))
        assertTrue(lShape.contains(2, 70, 2))
        assertFalse(lShape.contains(2, 59, 2))
        assertFalse(lShape.contains(2, 71, 2))
    }

    @Test
    fun `bounds report intersection, union and volume`() {
        val a = Bounds(0, 0, 0, 4, 4, 4)
        val b = Bounds(4, 4, 4, 8, 8, 8)
        val c = Bounds(5, 0, 0, 8, 4, 4)
        assertTrue(a.intersects(b))
        assertEquals(Bounds(4, 4, 4, 4, 4, 4), a.intersection(b))
        assertFalse(a.intersects(c))
        assertNull(a.intersection(c))
        assertEquals(Bounds(0, 0, 0, 8, 8, 8), a.union(b))
        assertEquals(125L, a.volume)
        assertEquals(1L, Bounds(3, 3, 3, 3, 3, 3).volume)
    }

    @Test
    fun `nested exclusions are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            shape {
                cuboid(0, 0, 0, 9, 9, 9)
                exclude { exclude { cuboid(1, 1, 1, 2, 2, 2) } }
            }
        }
    }
}
