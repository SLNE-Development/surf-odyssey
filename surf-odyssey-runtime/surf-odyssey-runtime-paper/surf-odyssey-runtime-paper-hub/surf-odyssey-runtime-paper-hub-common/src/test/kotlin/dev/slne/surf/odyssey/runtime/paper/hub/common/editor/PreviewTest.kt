package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks how many particles [Preview] draws along an edge and which edges it skips. */
class PreviewTest {
    @Test
    fun `an edge gets a step every half block`() = assertEquals(20, Preview.steps(10.0))

    @Test
    fun `a zero length edge still gets one step`() = assertEquals(1, Preview.steps(0.0))

    @Test
    fun `long edges are capped`() = assertEquals(Preview.MAX_STEPS, Preview.steps(10_000.0))

    @Test
    fun `the distance to an edge is measured to its nearest point`() {
        val a = Vec(0.0, 0.0, 0.0)
        val b = Vec(10.0, 0.0, 0.0)
        assertEquals(3.0, Preview.distance(Vec(5.0, 3.0, 0.0), a, b), 1e-9)
        assertEquals(4.0, Preview.distance(Vec(14.0, 0.0, 0.0), a, b), 1e-9)
        assertEquals(5.0, Preview.distance(Vec(-3.0, 4.0, 0.0), a, b), 1e-9)
        assertEquals(2.0, Preview.distance(Vec(0.0, 2.0, 0.0), a, a), 1e-9)
    }
}
