package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RestoreInBatchesTest {
    @Test
    fun `writes every index exactly once and pauses between batches`() = runTest {
        val written = mutableListOf<Int>()
        val completed = restoreInBatches(10, 4, { false }) { written += it }

        assertTrue(completed)
        assertEquals((0 until 10).toList(), written)
        assertEquals(150L, currentTime)
    }

    @Test
    fun `stops before the next batch when the abort flips`() = runTest {
        val written = mutableListOf<Int>()
        var abort = false
        val completed = restoreInBatches(10, 4, { abort }) {
            written += it
            if (it == 3) abort = true
        }

        assertFalse(completed)
        assertEquals(listOf(0, 1, 2, 3), written)
    }

    @Test
    fun `an abort before the first batch writes nothing`() = runTest {
        val written = mutableListOf<Int>()
        assertFalse(restoreInBatches(10, 4, { true }) { written += it })
        assertEquals(emptyList(), written)
    }

    @Test
    fun `a batch size of zero or less is treated as one`() = runTest {
        val written = mutableListOf<Int>()
        restoreInBatches(3, 0, { false }) { written += it }
        restoreInBatches(3, -5, { false }) { written += it }
        assertEquals(listOf(0, 1, 2, 0, 1, 2), written)
    }
}
