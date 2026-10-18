package dev.slne.surf.odyssey.runtime.paper.hub.common.gate

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which locked region each player is in, since when, when they last took damage there and whether they
 * were darkened. Thread-safe.
 */
class GateTracker {
    /**
     * The locked region a player is in.
     *
     * @property regionId the region
     * @property enteredAt when the player entered it, in epoch milliseconds
     * @property lastDamageAt when the player last took damage in it, or `null` if not yet
     */
    private class Inside(val regionId: String, val enteredAt: Long, var lastDamageAt: Long? = null)

    /** The locked region each player is in. */
    private val inside = ConcurrentHashMap<UUID, Inside>()

    /** The players marked as darkened. */
    private val darkened = ConcurrentHashMap.newKeySet<UUID>()

    /**
     * Records that [player] is inside the locked region [regionId] at [now] and is darkened there. Entering a region
     * other than the recorded one starts a new stay.
     *
     * @param player the player
     * @param regionId the locked region
     * @param damage the damage schedule of the region
     * @param now the current time in epoch milliseconds
     * @return whether the player takes damage now; if so, the damage is recorded
     */
    fun inLockedRegion(player: UUID, regionId: String, damage: GateDamage, now: Long): Boolean {
        val stay = inside[player]?.takeIf { it.regionId == regionId } ?: Inside(regionId, now).also { inside[player] = it }
        darkened += player
        if (!GatePlan.damageDue(stay.enteredAt, stay.lastDamageAt, now, damage)) return false
        stay.lastDamageAt = now
        return true
    }

    /**
     * Records that [player] is in no locked region and ends their stay.
     *
     * @param player the player
     * @return whether the player was marked as darkened, so the darkness must be removed
     */
    fun release(player: UUID): Boolean {
        inside.remove(player)
        return darkened.remove(player)
    }

    /**
     * Forgets every player.
     *
     * @return the players that were marked as darkened
     */
    fun clear(): Set<UUID> {
        val released = darkened.toSet()
        darkened.clear()
        inside.clear()
        return released
    }
}
