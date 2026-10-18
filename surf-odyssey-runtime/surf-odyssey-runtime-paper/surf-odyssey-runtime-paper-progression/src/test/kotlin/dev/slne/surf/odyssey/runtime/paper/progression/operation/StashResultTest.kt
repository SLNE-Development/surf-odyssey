package dev.slne.surf.odyssey.runtime.paper.progression.operation

import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for telling how a direct pickup box stash ended. */
class StashResultTest {
    @Test
    fun `a stored stash is in the pickup box`() {
        assertEquals(StashResult.STORED, StashResult.of(stored = true, holderPresent = true))
        assertEquals(StashResult.STORED, StashResult.of(stored = true, holderPresent = false))
    }

    @Test
    fun `a failed stash for a present holder was dropped at the feet`() {
        assertEquals(StashResult.DROPPED, StashResult.of(stored = false, holderPresent = true))
    }

    @Test
    fun `a failed stash for an absent holder is lost`() {
        assertEquals(StashResult.LOST, StashResult.of(stored = false, holderPresent = false))
    }
}
