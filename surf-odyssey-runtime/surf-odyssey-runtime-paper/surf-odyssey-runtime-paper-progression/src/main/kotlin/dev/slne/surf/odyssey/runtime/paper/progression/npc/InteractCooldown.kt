package dev.slne.surf.odyssey.runtime.paper.progression.npc

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

/**
 * Allows one NPC interaction per player within [window]. Thread-safe.
 *
 * @param window the time after an allowed interaction during which further interactions of the player are refused
 * @param nanoTime the monotonic clock in nanoseconds
 */
class InteractCooldown(private val window: Duration, private val nanoTime: () -> Long = System::nanoTime) {
    /** The time of the last allowed interaction of each player. */
    private val last = ConcurrentHashMap<UUID, Long>()

    /**
     * Records an interaction of [player] if the window since the last allowed one has passed.
     *
     * @return `true` if the interaction is allowed, `false` if it falls within the window
     */
    fun tryAcquire(player: UUID): Boolean {
        val now = nanoTime()
        var allowed = false
        last.compute(player) { _, previous ->
            if (previous == null || now - previous >= window.inWholeNanoseconds) {
                allowed = true
                now
            } else {
                previous
            }
        }
        return allowed
    }

    /** Drops the window of [player], so that the next interaction is allowed. */
    fun forget(player: UUID) {
        last.remove(player)
    }
}
