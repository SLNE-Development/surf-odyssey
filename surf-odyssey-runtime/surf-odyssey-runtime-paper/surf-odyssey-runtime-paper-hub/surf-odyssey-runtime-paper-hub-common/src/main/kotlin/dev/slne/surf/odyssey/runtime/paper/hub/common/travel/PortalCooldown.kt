package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Lets each player use portals at most once per [millis]. */
class PortalCooldown(private val millis: Long) {
    private val lastUse = ConcurrentHashMap<UUID, Long>()

    /** Returns `true` and starts the cooldown if [player] may use a portal at [now]. */
    fun tryUse(player: UUID, now: Long): Boolean {
        val last = lastUse[player]
        if (last != null && now - last < millis) return false
        lastUse[player] = now
        return true
    }

    /** Forgets [player]. */
    fun clear(player: UUID) {
        lastUse.remove(player)
    }
}
