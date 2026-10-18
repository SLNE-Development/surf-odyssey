package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Tracks which players have a transfer running, so that each player has at most one at a time. */
class TransfersInFlight {
    private val running = ConcurrentHashMap.newKeySet<UUID>()

    /** Marks a transfer of [player] as running and returns `true`, or returns `false` if one is already running. */
    fun tryStart(player: UUID): Boolean = running.add(player)

    /** Returns whether a transfer of [player] is running. */
    fun isRunning(player: UUID): Boolean = player in running

    /** Marks the transfer of [player] as finished. */
    fun finish(player: UUID) {
        running.remove(player)
    }
}
