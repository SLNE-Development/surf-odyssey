package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Point2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorSessionTest {
    @Test
    fun `a point added to a prism keeps only x and z`() {
        val session = EditorSession("test")
        session.startPrism(minY = 0, maxY = 10, excluded = false)
        assertTrue(session.addPoint(BlockPoint(3, 64, 7)))
        assertEquals(listOf(Point2(3, 7)), (session.parts.single() as PrismPart).points)
    }

    @Test
    fun `a cuboid takes at most two corners`() {
        val session = EditorSession("test")
        session.startCuboid(excluded = false)
        assertTrue(session.addPoint(BlockPoint(0, 0, 0)))
        assertTrue(session.addPoint(BlockPoint(1, 1, 1)))
        assertFalse(session.addPoint(BlockPoint(2, 2, 2)))
        assertEquals(listOf(BlockPoint(0, 0, 0), BlockPoint(1, 1, 1)), (session.parts.single() as CuboidPart).corners)
    }

    @Test
    fun `a point needs a started part`() {
        assertFalse(EditorSession("test").addPoint(BlockPoint(0, 0, 0)))
    }

    @Test
    fun `points go to the newest part`() {
        val session = EditorSession("test")
        session.startCuboid(excluded = false)
        session.startPrism(minY = 0, maxY = 10, excluded = true)
        session.addPoint(BlockPoint(5, 0, 6))
        assertTrue((session.parts[0] as CuboidPart).corners.isEmpty())
        assertEquals(listOf(Point2(5, 6)), (session.parts[1] as PrismPart).points)
    }

    @Test
    fun `undo removes the newest point and then the empty part`() {
        val session = EditorSession("test")
        session.startCuboid(excluded = false)
        session.addPoint(BlockPoint(0, 0, 0))
        assertTrue(session.undo())
        assertEquals(1, session.parts.size)
        assertTrue(session.undo())
        assertTrue(session.parts.isEmpty())
        assertFalse(session.undo())
    }
}
