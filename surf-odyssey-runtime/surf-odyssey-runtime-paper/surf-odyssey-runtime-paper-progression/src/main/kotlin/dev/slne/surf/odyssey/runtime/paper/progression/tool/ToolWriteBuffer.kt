package dev.slne.surf.odyssey.runtime.paper.progression.tool

import java.util.*

/**
 * Holds tool usage and chronicle events in memory until they are written to the tool item, and decides when that
 * write happens: at once for a tool that was never written or that levels up, otherwise at most once per
 * [intervalMillis]. All functions are thread-safe.
 *
 * @param intervalMillis the shortest time between two writes of the same tool without a level-up
 */
class ToolWriteBuffer(private val intervalMillis: Long = 1_000) {
    /**
     * The changes of one tool that are not written yet.
     *
     * @property usage the added usage
     * @property events the chronicle events, oldest first
     */
    data class Delta(val usage: Long, val events: List<ChronicleEvent>)

    /**
     * The buffered changes of one tool.
     *
     * @property holder the player who made the newest change
     * @property usage the added usage
     * @property events the chronicle events, oldest first
     */
    private class Entry(var holder: UUID, var usage: Long, val events: MutableList<ChronicleEvent>)

    /** The buffered changes by tool UUID. */
    private val entries = HashMap<UUID, Entry>()

    /** The time of the last write by tool UUID. */
    private val writtenAt = HashMap<UUID, Long>()

    /**
     * Buffers [usage] and [event] for [tool], made by [holder].
     *
     * @param tool the unique ID of the tool item
     * @param holder the player who used the tool
     * @param usage the added usage
     * @param event the chronicle event, or `null`
     */
    @Synchronized
    fun add(tool: UUID, holder: UUID, usage: Long, event: ChronicleEvent?) {
        val entry = entries.getOrPut(tool) { Entry(holder, 0, mutableListOf()) }
        entry.holder = holder
        entry.usage += usage
        if (event != null) entry.events += event
    }

    /** Returns whether changes of [tool] are buffered. */
    @Synchronized
    fun hasPending(tool: UUID): Boolean = tool in entries

    /** Returns the usage buffered for [tool], or 0. */
    @Synchronized
    fun pendingUsage(tool: UUID): Long = entries[tool]?.usage ?: 0

    /**
     * Returns whether the buffered changes of [tool] are written now: always on [leveledUp] or when the tool was never
     * written, otherwise when the last write is at least the interval before [now].
     *
     * @param tool the unique ID of the tool item
     * @param now the current time in epoch milliseconds
     * @param leveledUp whether the newest change raised the level of the tool
     */
    @Synchronized
    fun shouldWriteNow(tool: UUID, now: Long, leveledUp: Boolean): Boolean = leveledUp || isDue(tool, now)

    /**
     * Removes and returns the buffered changes of [tool] and remembers [now] as its last write.
     *
     * @param tool the unique ID of the tool item
     * @param now the current time in epoch milliseconds
     * @return the changes, or `null` if nothing was buffered
     */
    @Synchronized
    fun drain(tool: UUID, now: Long): Delta? {
        writtenAt[tool] = now
        val entry = entries.remove(tool) ?: return null
        return Delta(entry.usage, entry.events.toList())
    }

    /**
     * Returns every buffered tool with its newest holder whose last write is at least the interval before [now].
     *
     * @param now the current time in epoch milliseconds
     */
    @Synchronized
    fun due(now: Long): List<Pair<UUID, UUID>> =
        entries.filterKeys { isDue(it, now) }.map { (tool, entry) -> tool to entry.holder }

    /**
     * Forgets the last write time of [tool] when nothing is buffered for it.
     *
     * @param tool the unique ID of the tool item
     */
    @Synchronized
    fun forget(tool: UUID) {
        if (tool !in entries) writtenAt.remove(tool)
    }

    /** Returns whether [tool] was never written or its last write is at least the interval before [now]. */
    private fun isDue(tool: UUID, now: Long): Boolean {
        val last = writtenAt[tool] ?: return true
        return now - last >= intervalMillis
    }
}
