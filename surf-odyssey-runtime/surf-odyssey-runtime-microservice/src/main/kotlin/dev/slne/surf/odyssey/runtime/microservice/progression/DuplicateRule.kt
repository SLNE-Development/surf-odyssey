package dev.slne.surf.odyssey.runtime.microservice.progression

import java.util.*

/**
 * One report that a player held a tool.
 *
 * @property holder the player who held the tool
 * @property at the epoch milliseconds the tool was seen at
 */
data class Sighting(val holder: UUID, val at: Long)

/** Decides from recent sightings whether a tool item has probably been duplicated. */
object DuplicateRule {
    /** How far back, in milliseconds, sightings are taken into account. */
    const val WINDOW_MILLIS = 120_000L

    /** How long, in milliseconds, sightings are kept before they are deleted. */
    const val RETENTION_MILLIS = 600_000L

    /** How many changes of the holder within [WINDOW_MILLIS] make a duplicate suspected. */
    private const val SUSPICIOUS_HOLDER_CHANGES = 2

    /**
     * Returns `true` if the holder of the tool changed at least twice within the last [WINDOW_MILLIS] before [now],
     * such as a hand-over from A to B and back to A. A sighting exactly [WINDOW_MILLIS] old still counts.
     */
    fun isSuspected(sightings: List<Sighting>, now: Long): Boolean {
        val recent = sightings.filter { it.at >= now - WINDOW_MILLIS }.sortedBy { it.at }
        val changes = recent.zipWithNext().count { (previous, next) -> previous.holder != next.holder }
        return changes >= SUSPICIOUS_HOLDER_CHANGES
    }
}
