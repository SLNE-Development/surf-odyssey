package dev.slne.surf.odyssey.runtime.paper.hub.common.gate

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which locked region each player is in, since when, when they last took damage there and whether they
 * were darkened, and how far the world gate check of each player got. Thread-safe.
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

    /** The players whose world gate check is finished since they joined. */
    private val worldPassed = ConcurrentHashMap.newKeySet<UUID>()

    /** The players whose transfer to the lobby is running. */
    private val worldTransfers = ConcurrentHashMap.newKeySet<UUID>()

    /** When the last transfer to the lobby of each player started, in epoch milliseconds. */
    private val worldAttempts = ConcurrentHashMap<UUID, Long>()

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
     * Returns whether the world gate check of [player] is finished.
     *
     * @param player the player
     */
    fun isWorldGatePassed(player: UUID): Boolean = player in worldPassed

    /**
     * Finishes the world gate check of [player] without a transfer.
     *
     * @param player the player
     */
    fun markWorldGatePassed(player: UUID) {
        worldPassed += player
    }

    /**
     * Starts a transfer of [player] to the lobby unless one is running or the last one started less than
     * [retryAfterMillis] before [now].
     *
     * @param player the player
     * @param now the current time in epoch milliseconds
     * @param retryAfterMillis the shortest time between two transfers
     * @return whether the transfer may start; if so, it is recorded as running
     */
    fun tryStartWorldTransfer(player: UUID, now: Long, retryAfterMillis: Long): Boolean {
        val last = worldAttempts[player]
        if (last != null && now - last < retryAfterMillis) return false
        if (!worldTransfers.add(player)) return false
        worldAttempts[player] = now
        return true
    }

    /**
     * Ends the running transfer of [player]. A successful transfer finishes the world gate check; after a failed one
     * the next transfer may start once the retry interval has passed.
     *
     * @param player the player
     * @param sent whether the player was transferred
     */
    fun finishWorldTransfer(player: UUID, sent: Boolean) {
        worldTransfers.remove(player)
        if (sent) worldPassed += player
    }

    /**
     * Forgets everything about [player], as when they leave.
     *
     * @param player the player
     * @return whether the player was marked as darkened, so the darkness must be removed
     */
    fun forget(player: UUID): Boolean {
        worldPassed.remove(player)
        worldTransfers.remove(player)
        worldAttempts.remove(player)
        return release(player)
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
        worldPassed.clear()
        worldTransfers.clear()
        worldAttempts.clear()
        return released
    }
}
