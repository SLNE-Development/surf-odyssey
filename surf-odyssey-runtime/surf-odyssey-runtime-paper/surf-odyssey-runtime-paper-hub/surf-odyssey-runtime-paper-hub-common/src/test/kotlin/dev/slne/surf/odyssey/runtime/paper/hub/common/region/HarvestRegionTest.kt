package dev.slne.surf.odyssey.runtime.paper.hub.common.region

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Checks the limits [HarvestRegion] puts on its settings. */
class HarvestRegionTest {
    private fun mine(delay: Duration) = RegionRegistry().apply {
        mine("m") {
            shape { cuboid(0, 0, 0, 1, 1, 1) }
            regenerateAfter = delay
        }
    }.build().single() as HarvestRegion

    @Test
    fun `blocks regenerate after at least one second`() {
        assertEquals(1.seconds, mine(1.seconds).regenerateAfter)
        assertFailsWith<IllegalArgumentException> { mine(999.milliseconds) }
        assertFailsWith<IllegalArgumentException> { mine(Duration.ZERO) }
    }
}
