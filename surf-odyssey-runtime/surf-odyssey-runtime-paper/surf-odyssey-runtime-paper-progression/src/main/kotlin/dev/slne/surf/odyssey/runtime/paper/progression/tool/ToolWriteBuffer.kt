package dev.slne.surf.odyssey.runtime.paper.progression.tool

import java.util.*

/**
 * Holds tool usage and chronicle events in memory until they are written to the tool item, separately for each
 * holder and tool UUID, and decides when that write happens: at once for an entry that was never written or that
 * levels up, otherwise at most once per [intervalMillis]. All functions are thread-safe.
 *
 * @param intervalMillis the shortest time between two writes of the same entry without a level-up
 */
class ToolWriteBuffer(private val intervalMillis: Long = 1_000) {
    /**
     * The changes of one entry that are not written yet.
     *
     * @property usage the added usage
     * @property events the chronicle events, oldest first
     */
    data class Delta(val usage: Long, val events: List<ChronicleEvent>)

    /**
     * Identifies the changes one holder made to one tool item.
     *
     * @property holder the player who used the tool
     * @property tool the unique ID of the tool item
     */
    private data class Key(val holder: UUID, val tool: UUID)

    /**
     * The buffered changes of one entry.
     *
     * @property slot the inventory slot the tool was in at the newest change
     * @property usage the added usage
     * @property events the chronicle events, oldest first
     */
    private class Entry(var slot: Int, var usage: Long, val events: MutableList<ChronicleEvent>)

    /** The buffered changes. */
    private val entries = HashMap<Key, Entry>()

    /** The time of the last write of each entry. */
    private val writtenAt = HashMap<Key, Long>()

    /**
     * Buffers [usage] and [event] that [holder] made with [tool] while it was in inventory slot [slot].
     *
     * @param holder the player who used the tool
     * @param tool the unique ID of the tool item
     * @param slot the inventory slot of the tool
     * @param usage the added usage
     * @param event the chronicle event, or `null`
     */
    @Synchronized
    fun add(holder: UUID, tool: UUID, slot: Int, usage: Long, event: ChronicleEvent?) {
        val entry = entries.getOrPut(Key(holder, tool)) { Entry(slot, 0, mutableListOf()) }
        entry.slot = slot
        entry.usage += usage
        if (event != null) entry.events += event
    }

    /** Returns whether changes of [holder] to [tool] are buffered. */
    @Synchronized
    fun hasPending(holder: UUID, tool: UUID): Boolean = Key(holder, tool) in entries

    /** Returns the usage [holder] buffered for [tool], or 0. */
    @Synchronized
    fun pendingUsage(holder: UUID, tool: UUID): Long = entries[Key(holder, tool)]?.usage ?: 0

    /** Returns the inventory slot [tool] was in at the newest buffered change of [holder], or `null`. */
    @Synchronized
    fun slotOf(holder: UUID, tool: UUID): Int? = entries[Key(holder, tool)]?.slot

    /** Returns the tools with buffered changes of [holder]. */
    @Synchronized
    fun pendingOf(holder: UUID): List<UUID> = entries.keys.filter { it.holder == holder }.map { it.tool }

    /**
     * Returns whether the buffered changes of [holder] to [tool] are written now: always on [leveledUp] or when the
     * entry was never written, otherwise when its last write is at least the interval before [now].
     *
     * @param holder the player who used the tool
     * @param tool the unique ID of the tool item
     * @param now the current time in epoch milliseconds
     * @param leveledUp whether the newest change raised the level of the tool
     */
    @Synchronized
    fun shouldWriteNow(holder: UUID, tool: UUID, now: Long, leveledUp: Boolean): Boolean =
        leveledUp || isDue(Key(holder, tool), now)

    /**
     * Removes and returns the buffered changes of [holder] to [tool] and remembers [now] as their last write.
     *
     * @param holder the player who used the tool
     * @param tool the unique ID of the tool item
     * @param now the current time in epoch milliseconds
     * @return the changes, or `null` if nothing was buffered
     */
    @Synchronized
    fun drain(holder: UUID, tool: UUID, now: Long): Delta? {
        val key = Key(holder, tool)
        writtenAt[key] = now
        val entry = entries.remove(key) ?: return null
        return Delta(entry.usage, entry.events.toList())
    }

    /**
     * Returns every buffered entry as holder and tool whose last write is at least the interval before [now].
     *
     * @param now the current time in epoch milliseconds
     */
    @Synchronized
    fun due(now: Long): List<Pair<UUID, UUID>> = entries.keys.filter { isDue(it, now) }.map { it.holder to it.tool }

    /**
     * Drops every buffered change and write time of [holder].
     *
     * @param holder the player whose entries are dropped
     * @return the tools whose buffered changes were dropped
     */
    @Synchronized
    fun discard(holder: UUID): List<UUID> {
        writtenAt.keys.removeIf { it.holder == holder }
        val dropped = entries.keys.filter { it.holder == holder }
        dropped.forEach(entries::remove)
        return dropped.map { it.tool }
    }

    /** Returns whether [key] was never written or its last write is at least the interval before [now]. */
    private fun isDue(key: Key, now: Long): Boolean {
        val last = writtenAt[key] ?: return true
        return now - last >= intervalMillis
    }
}
