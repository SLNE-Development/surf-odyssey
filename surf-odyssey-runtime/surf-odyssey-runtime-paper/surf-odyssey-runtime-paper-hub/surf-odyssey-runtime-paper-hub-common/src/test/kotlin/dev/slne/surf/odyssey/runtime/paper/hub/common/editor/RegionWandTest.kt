package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks that [RegionWand] marks wands with the same key on every hub server. */
class RegionWandTest {
    @Test
    fun `the wand key lives in the fixed odyssey namespace`() =
        assertEquals("odyssey:region_wand", RegionWand.KEY.asString())
}
