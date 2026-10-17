package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Point2
import kotlin.test.Test
import kotlin.test.assertEquals

class ShapeOutlineTest {
    @Test
    fun `a cuboid has twelve edges`() =
        assertEquals(12, ShapeOutline.edges(CuboidPart(mutableListOf(BlockPoint(0, 0, 0), BlockPoint(2, 2, 2)), excluded = false)).size)

    @Test
    fun `a triangle prism has nine edges`() =
        assertEquals(9, ShapeOutline.edges(PrismPart(0, 4, mutableListOf(Point2(0, 0), Point2(4, 0), Point2(0, 4)), excluded = false)).size)

    @Test
    fun `an unfinished part draws nothing`() =
        assertEquals(0, ShapeOutline.edges(CuboidPart(mutableListOf(BlockPoint(0, 0, 0)), excluded = false)).size)
}
