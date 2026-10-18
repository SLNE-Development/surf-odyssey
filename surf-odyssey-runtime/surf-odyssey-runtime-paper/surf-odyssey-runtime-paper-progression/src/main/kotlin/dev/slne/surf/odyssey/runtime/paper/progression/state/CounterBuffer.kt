package dev.slne.surf.odyssey.runtime.paper.progression.state

import dev.slne.surf.odyssey.core.common.progression.CounterDeltaDto
import dev.slne.surf.odyssey.core.common.progression.CounterDto

/**
 * Collects counter deltas of one player until they are sent to the progression service.
 *
 * Deltas for the same counter (quest, step, step version, objective and period) are summed into one. All functions
 * are safe to call from several threads.
 */
class CounterBuffer {
    /** The summed deltas by the counter they belong to. */
    private val deltas = LinkedHashMap<CounterKey, Int>()

    /**
     * Adds [delta] to the buffered amount of its counter.
     *
     * @param delta the delta to add
     */
    @Synchronized
    fun add(delta: CounterDeltaDto) {
        val key = CounterKey.of(delta)
        deltas[key] = (deltas[key] ?: 0) + delta.delta
    }

    /**
     * Adds deltas that could not be sent back into the buffer, summed with any deltas buffered since.
     *
     * @param failed the deltas of the failed attempt
     */
    @Synchronized
    fun requeue(failed: List<CounterDeltaDto>) {
        failed.forEach(::add)
    }

    /**
     * Returns all buffered deltas and empties the buffer.
     *
     * @return one delta per counter, in the order the counters were first buffered
     */
    @Synchronized
    fun drain(): List<CounterDeltaDto> {
        val drained = pending()
        deltas.clear()
        return drained
    }

    /**
     * Returns all buffered deltas without removing them.
     *
     * @return one delta per counter, in the order the counters were first buffered
     */
    @Synchronized
    fun pending(): List<CounterDeltaDto> = deltas.map { (key, amount) -> key.toDelta(amount) }

    /**
     * Drops every buffered delta of the quest [questId].
     *
     * @param questId the id of the quest definition
     */
    @Synchronized
    fun discard(questId: String) {
        deltas.keys.removeIf { it.questId == questId }
    }

    /** Returns whether no delta is buffered. */
    @Synchronized
    fun isEmpty(): Boolean = deltas.isEmpty()
}

/**
 * Identifies one objective counter.
 *
 * @property questId the id of the quest definition
 * @property step the index of the step the objective belongs to
 * @property stepVersion the version of the step
 * @property objective the index of the objective within the step
 * @property period the period index the counter belongs to
 */
internal data class CounterKey(
    val questId: String,
    val step: Int,
    val stepVersion: Int,
    val objective: Int,
    val period: Long
) {
    /** Creates a delta of [amount] for this counter. */
    fun toDelta(amount: Int) = CounterDeltaDto(questId, step, stepVersion, objective, period, amount)

    /** Creates counter keys from transfer objects. */
    companion object {
        /** Returns the key of the counter [delta] belongs to. */
        fun of(delta: CounterDeltaDto) =
            CounterKey(delta.questId, delta.step, delta.stepVersion, delta.objective, delta.period)

        /** Returns the key of the counter [counter] stores. */
        fun of(counter: CounterDto) =
            CounterKey(counter.questId, counter.step, counter.stepVersion, counter.objective, counter.period)
    }
}
