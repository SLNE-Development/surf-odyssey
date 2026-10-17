package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks how [WaterDepth] counts the water under a fishing hook. */
class WaterDepthTest {
    /** Returns a column where the blocks at the offsets in [water] (0 = hook block, negative = below) are water. */
    private fun column(vararg water: Int): (Int) -> Boolean = { it in water }

    @Test
    fun `water is counted from the hook block downwards`() = assertEquals(3, WaterDepth.count(column(0, -1, -2), 16))

    @Test
    fun `a hook above the water counts from the block below`() = assertEquals(2, WaterDepth.count(column(-1, -2), 16))

    @Test
    fun `no water under the hook gives zero`() = assertEquals(0, WaterDepth.count(column(), 16))

    @Test
    fun `counting stops at the first block that is not water`() = assertEquals(1, WaterDepth.count(column(0, -2, -3), 16))

    @Test
    fun `counting stops at the limit`() = assertEquals(4, WaterDepth.count({ true }, 4))
}
