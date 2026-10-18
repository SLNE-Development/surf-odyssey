package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import org.bukkit.entity.FishHook

/** Shortens the time a fishing hook waits for a bite by the bite speed bonus of the rod. */
object BiteSpeed {
    /**
     * Returns [ticks] shortened by [percent] percent, at least 1 tick.
     *
     * @param ticks the wait without bonus
     * @param percent the bite speed bonus, between 0 and 100
     */
    fun scaledWait(ticks: Int, percent: Int): Int = (ticks.toLong() * (100 - percent) / 100).toInt().coerceAtLeast(1)

    /**
     * Shortens the minimum and maximum wait of [hook] by [percent] percent. Does nothing for a bonus of 0.
     *
     * @param hook the hook that was cast
     * @param percent the bite speed bonus, between 0 and 100
     */
    fun apply(hook: FishHook, percent: Int) {
        if (percent <= 0) return
        hook.setWaitTime(scaledWait(hook.minWaitTime, percent), scaledWait(hook.maxWaitTime, percent))
    }
}
