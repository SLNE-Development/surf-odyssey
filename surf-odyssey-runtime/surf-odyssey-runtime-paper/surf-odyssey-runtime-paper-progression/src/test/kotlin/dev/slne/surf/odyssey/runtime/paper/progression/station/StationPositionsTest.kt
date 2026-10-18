package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests for looking up station placements by block position. */
class StationPositionsTest {
    /** The world the placements belong to. */
    private val hub = UUID.randomUUID()

    /** The station placed in [hub]. */
    private val station = TestPacks.sushiRecipe(rice = 1, fish = 1).station

    /** One placement at 10, 64, -3 in [hub]. */
    private val positions = StationPositions(hub, listOf(StationPlacement(station, 10, 64, -3)))

    @Test
    fun `a placement is found at its position in its world`() =
        assertEquals(station, positions.stationAt(hub, 10, 64, -3))

    @Test
    fun `the same position in another world is no station`() =
        assertNull(positions.stationAt(UUID.randomUUID(), 10, 64, -3))

    @Test
    fun `other positions are no station`() =
        assertNull(positions.stationAt(hub, 10, 65, -3))
}
