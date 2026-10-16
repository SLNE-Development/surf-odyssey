package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val COUNTDOWN_SECONDS = 5

fun warningTimes(configured: List<Long>): List<Duration> =
    (configured + (1L..COUNTDOWN_SECONDS)).distinct().map { it.seconds }.sortedDescending()

fun countdownPitch(secondsLeft: Int): Float {
    val step = (COUNTDOWN_SECONDS - secondsLeft.coerceIn(1, COUNTDOWN_SECONDS)).toFloat() / (COUNTDOWN_SECONDS - 1)
    return 1.0f + 0.6f * step
}
