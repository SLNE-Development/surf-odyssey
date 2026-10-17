package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MiningResetCycleTest {
    private val events = mutableListOf<Pair<Long, String>>()

    private fun TestScope.cycle(scope: CoroutineScope, interval: Duration = 60.minutes) = MiningResetCycle(
        scope = scope,
        interval = interval,
        warnings = listOf(60.seconds, 30.seconds, 10.seconds, 5.seconds),
        warn = { events += currentTime to "warn ${it.inWholeSeconds}" },
        reset = { events += currentTime to "reset" },
    )

    @Test
    fun `warns before and resets at the end of each interval`() = runTest {
        cycle(backgroundScope).start()
        advanceTimeBy(60.minutes.inWholeMilliseconds + 1); runCurrent()

        assertEquals(
            listOf(
                3_540_000L to "warn 60", 3_570_000L to "warn 30", 3_590_000L to "warn 10",
                3_595_000L to "warn 5", 3_600_000L to "reset",
            ),
            events,
        )
    }

    @Test
    fun `trigger now counts down immediately and restarts the interval`() = runTest {
        val cycle = cycle(backgroundScope)
        cycle.start()
        advanceTimeBy(10.minutes.inWholeMilliseconds); runCurrent()
        cycle.triggerNow(); runCurrent()
        advanceTimeBy(60.seconds.inWholeMilliseconds + 1); runCurrent()

        assertEquals(600_000L to "warn 60", events.first())
        assertEquals(660_000L to "reset", events.last())

        events.clear()
        advanceTimeBy(60.minutes.inWholeMilliseconds); runCurrent()
        assertEquals(660_000L + 3_600_000L to "reset", events.last())
    }

    @Test
    fun `warnings longer than the interval are capped`() = runTest {
        cycle(backgroundScope, interval = 20.seconds).start()
        advanceTimeBy(20_001); runCurrent()

        assertEquals(listOf(10_000L to "warn 10", 15_000L to "warn 5", 20_000L to "reset"), events)
    }

    @Test
    fun `stop cancels the cycle`() = runTest {
        val cycle = cycle(backgroundScope)
        cycle.start()
        cycle.stop()
        advanceTimeBy(2 * 60.minutes.inWholeMilliseconds); runCurrent()

        assertEquals(emptyList(), events)
    }

    @Test
    fun `a failing reset does not stop the cycle`() = runTest {
        val failures = mutableListOf<Throwable>()
        var calls = 0
        MiningResetCycle(
            scope = backgroundScope,
            interval = 20.seconds,
            warnings = emptyList(),
            warn = {},
            reset = {
                calls++
                if (calls == 1) error("boom")
                events += currentTime to "reset"
            },
            onFailure = { failures += it },
        ).start()
        advanceTimeBy(40_001); runCurrent()

        assertEquals(1, failures.size)
        assertEquals(listOf(40_000L to "reset"), events)
    }

    @Test
    fun `trigger now does not interrupt a running reset`() = runTest {
        var started = 0
        val cycle = MiningResetCycle(
            scope = backgroundScope,
            interval = 20.seconds,
            warnings = emptyList(),
            warn = {},
            reset = {
                started++
                delay(5_000)
                events += currentTime to "reset"
            },
        )
        cycle.start()
        advanceTimeBy(21_000); runCurrent()

        assertTrue(cycle.isResetting)
        assertFalse(cycle.triggerNow())
        advanceTimeBy(5_000); runCurrent()

        assertEquals(1, started)
        assertEquals(listOf(25_000L to "reset"), events)
        assertTrue(cycle.triggerNow())
    }

    @Test
    fun `non positive and duplicate warnings are ignored`() = runTest {
        MiningResetCycle(
            scope = backgroundScope,
            interval = 20.seconds,
            warnings = listOf(10.seconds, 10.seconds, Duration.ZERO, (-5).seconds),
            warn = { events += currentTime to "warn ${it.inWholeSeconds}" },
            reset = { events += currentTime to "reset" },
        ).start()
        advanceTimeBy(20_001); runCurrent()

        assertEquals(listOf(10_000L to "warn 10", 20_000L to "reset"), events)
    }

    @Test
    fun `a foreign cancellation exception does not stop the cycle`() = runTest {
        val failures = mutableListOf<Throwable>()
        var calls = 0
        MiningResetCycle(
            scope = backgroundScope,
            interval = 20.seconds,
            warnings = emptyList(),
            warn = {},
            reset = {
                calls++
                if (calls == 1) throw CancellationException("cancelled future")
                events += currentTime to "reset"
            },
            onFailure = { failures += it },
        ).start()
        advanceTimeBy(40_001); runCurrent()

        assertEquals(1, failures.size)
        assertEquals(listOf(40_000L to "reset"), events)
    }
}
