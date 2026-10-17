package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

class MiningResetCycle(
    private val scope: CoroutineScope,
    private val interval: Duration,
    warnings: List<Duration>,
    private val warn: suspend (remaining: Duration) -> Unit,
    private val reset: suspend () -> Unit,
    private val onFailure: (Throwable) -> Unit = {},
) {
    private val warnings = warnings.filter { it > Duration.ZERO && it <= interval }.distinct().sortedDescending()
    private val lead = this.warnings.firstOrNull() ?: Duration.ZERO

    @Volatile
    private var job: Job? = null

    @Volatile
    private var resetRunning = false

    val isResetting: Boolean get() = resetRunning

    fun start() {
        if (resetRunning) return
        job?.cancel()
        job = scope.launch { loop() }
    }

    fun triggerNow(): Boolean {
        if (resetRunning) return false
        job?.cancel()
        job = scope.launch {
            countdown()
            loop()
        }
        return true
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun loop() {
        while (currentCoroutineContext().isActive) {
            delay(interval - lead)
            countdown()
        }
    }

    private suspend fun countdown() {
        var remaining = lead
        for (warning in warnings) {
            delay(remaining - warning)
            guarded { warn(warning) }
            remaining = warning
        }
        delay(remaining)
        resetRunning = true
        try {
            guarded { reset() }
        } finally {
            resetRunning = false
        }
    }

    private suspend fun guarded(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            onFailure(e)
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
