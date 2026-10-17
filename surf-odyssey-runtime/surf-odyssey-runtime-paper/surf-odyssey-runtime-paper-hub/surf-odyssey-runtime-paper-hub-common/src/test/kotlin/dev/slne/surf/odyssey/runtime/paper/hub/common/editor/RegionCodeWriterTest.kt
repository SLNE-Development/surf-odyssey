package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks the Kotlin source [RegionCodeWriter] produces for an editor session. */
class RegionCodeWriterTest {
    @Test
    fun `a session without excluded parts has no exclude block`() {
        val session = EditorSession("north_mine")
        session.startCuboid(excluded = false)
        session.addPoint(BlockPoint(1, 2, 3))
        session.addPoint(BlockPoint(4, 5, 6))
        assertEquals(
            """
            |// Region north_mine
            |shape {
            |    cuboid(1, 2, 3, 4, 5, 6)
            |}
            |""".trimMargin(),
            RegionCodeWriter.write(session),
        )
    }

    @Test
    fun `several included and excluded parts keep their order`() {
        val session = EditorSession("north_lake")
        session.startCuboid(excluded = false)
        session.addPoint(BlockPoint(0, 0, 0))
        session.addPoint(BlockPoint(9, 9, 9))
        session.startCuboid(excluded = true)
        session.addPoint(BlockPoint(1, 1, 1))
        session.addPoint(BlockPoint(2, 2, 2))
        session.startPrism(minY = 5, maxY = 1, excluded = false)
        session.addPoint(BlockPoint(20, 0, 20))
        session.addPoint(BlockPoint(30, 0, 20))
        session.addPoint(BlockPoint(30, 0, 30))
        session.startCuboid(excluded = true)
        session.addPoint(BlockPoint(3, 3, 3))
        session.addPoint(BlockPoint(4, 4, 4))
        assertEquals(
            """
            |// Region north_lake
            |shape {
            |    cuboid(0, 0, 0, 9, 9, 9)
            |    prism(minY = 1, maxY = 5) {
            |        point(20, 20)
            |        point(30, 20)
            |        point(30, 30)
            |    }
            |    exclude {
            |        cuboid(1, 1, 1, 2, 2, 2)
            |        cuboid(3, 3, 3, 4, 4, 4)
            |    }
            |}
            |""".trimMargin(),
            RegionCodeWriter.write(session),
        )
    }

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
