package dev.slne.surf.odyssey.runtime.paper.hub.common.regeneration

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint

/**
 * Blocks waiting to return to their [Entry.original] state at [Entry.dueAt] (epoch millis).
 *
 * Each block waits at most once: scheduling a waiting block again keeps the first entry.
 * Not thread-safe; used from the main thread only.
 */
class RegenerationQueue<T> {
    /**
     * A block that returns to [original] at [dueAt].
     *
     * @property point position of the block
     * @property original state the block returns to
     * @property dueAt epoch millis at which the block returns
     */
    data class Entry<T>(val point: BlockPoint, val original: T, val dueAt: Long)

    private val pending = HashMap<BlockPoint, Entry<T>>()

    /** Number of waiting blocks. */
    val size: Int get() = pending.size

    /** Schedules [point]; returns `false` and changes nothing if it is already waiting. */
    fun schedule(point: BlockPoint, original: T, dueAt: Long): Boolean {
        if (point in pending) return false
        pending[point] = Entry(point, original, dueAt)
        return true
    }

    /** Returns whether [point] is waiting to regenerate. */
    fun isPending(point: BlockPoint): Boolean = point in pending

    /** Removes and returns every entry due at [now], earliest first. */
    fun pollDue(now: Long): List<Entry<T>> {
        val due = pending.values.filter { it.dueAt <= now }.sortedBy { it.dueAt }
        due.forEach { pending.remove(it.point) }
        return due
    }
}
