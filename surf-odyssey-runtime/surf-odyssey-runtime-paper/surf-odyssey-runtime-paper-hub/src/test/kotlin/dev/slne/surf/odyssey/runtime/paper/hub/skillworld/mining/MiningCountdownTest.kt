package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class MiningCountdownTest {
    @Test
    fun `configured warnings are joined with the last five seconds`() {
        assertEquals(
            listOf(60, 30, 10, 5, 4, 3, 2, 1).map { it.seconds },
            warningTimes(listOf(60, 30, 10, 5)),
        )
    }

    @Test
    fun `warning times have no duplicates and keep other configured values`() {
        val times = warningTimes(listOf(3, 3, 45))
        assertEquals(times.distinct(), times)
        assertEquals(listOf(45, 5, 4, 3, 2, 1).map { it.seconds }, times)
    }

    @Test
    fun `the pitch rises from the fifth to the last second`() {
        assertEquals(1.0f, countdownPitch(5), 0.0001f)
        assertEquals(1.6f, countdownPitch(1), 0.0001f)
        val pitches = (5 downTo 1).map(::countdownPitch)
        assertTrue(pitches.zipWithNext().all { (a, b) -> b > a })
    }
}
