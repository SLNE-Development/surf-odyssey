package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Point2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks how an [EditorSession] collects parts and points and which problems it reports. */
class EditorSessionTest {
    @Test
    fun `an empty session needs an included part`() =
        assertEquals(listOf("Die Region hat noch keinen eingeschlossenen Teil."), EditorSession("test").problems())

    @Test
    fun `a session with only excluded parts needs an included part`() {
        val session = EditorSession("test")
        session.startCuboid(excluded = true)
        session.addPoint(BlockPoint(0, 0, 0))
        session.addPoint(BlockPoint(1, 1, 1))
        assertEquals(listOf("Die Region hat noch keinen eingeschlossenen Teil."), session.problems())
    }

    @Test
    fun `incomplete parts are reported by their number`() {
        val session = EditorSession("test")
        session.startPrism(minY = 0, maxY = 10, excluded = false)
        session.addPoint(BlockPoint(0, 0, 0))
        session.addPoint(BlockPoint(1, 0, 0))
        session.startCuboid(excluded = true)
        session.addPoint(BlockPoint(0, 0, 0))
        assertEquals(listOf("Teil 1 braucht mindestens drei Punkte.", "Teil 2 braucht zwei Ecken."), session.problems())
    }

    @Test
    fun `a complete session has no problems`() {
        val session = EditorSession("test")
        session.startCuboid(excluded = false)
        session.addPoint(BlockPoint(0, 0, 0))
        session.addPoint(BlockPoint(1, 1, 1))
        assertEquals(emptyList(), session.problems())
    }

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
    fun `removing the last point keeps the part`() {
        val session = EditorSession("test")
        session.startPrism(minY = 0, maxY = 10, excluded = false)
        session.addPoint(BlockPoint(1, 0, 1))
        session.addPoint(BlockPoint(2, 0, 2))
        assertTrue(session.removeLastPoint())
        assertEquals(listOf(Point2(1, 1)), (session.parts.single() as PrismPart).points)
        assertTrue(session.removeLastPoint())
        assertFalse(session.removeLastPoint())
        assertEquals(1, session.parts.size)
    }

    @Test
    fun `removing a point needs a started part`() {
        assertFalse(EditorSession("test").removeLastPoint())
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
