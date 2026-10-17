package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class RegionCodeWriterTest {
    @Test
    fun `a session becomes a ready to paste shape block`() {
        val session = EditorSession("north_forest_spruce")
        session.startPrism(minY = 60, maxY = 110, excluded = false)
        session.addPoint(BlockPoint(10, 64, 20))
        session.addPoint(BlockPoint(40, 70, 20))
        session.addPoint(BlockPoint(40, 66, 50))
        session.startCuboid(excluded = true)
        session.addPoint(BlockPoint(20, 60, 30))
        session.addPoint(BlockPoint(25, 70, 35))
        assertEquals(
            """
            |// Region north_forest_spruce
            |shape {
            |    prism(minY = 60, maxY = 110) {
            |        point(10, 20)
            |        point(40, 20)
            |        point(40, 50)
            |    }
            |    exclude {
            |        cuboid(20, 60, 30, 25, 70, 35)
            |    }
            |}
            |""".trimMargin(),
            RegionCodeWriter.write(session),
        )
    }
}
