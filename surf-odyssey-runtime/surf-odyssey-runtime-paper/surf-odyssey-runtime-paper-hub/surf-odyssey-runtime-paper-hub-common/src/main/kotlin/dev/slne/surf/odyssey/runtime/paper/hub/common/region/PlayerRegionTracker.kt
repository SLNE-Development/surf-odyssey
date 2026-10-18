package dev.slne.surf.odyssey.runtime.paper.hub.common.region

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Remembers which region each player stands in and calls the enter and leave hooks. */
object PlayerRegionTracker {
    private val current = ConcurrentHashMap<UUID, Region>()

    /** Returns the region [player] currently stands in, or `null`. */
    fun regionOf(player: Player): Region? = current[player.uniqueId]

    /** Records that [player] now stands in [region] and calls the hooks if it changed. */
    fun update(player: Player, region: Region?) {
        val previous = if (region == null) current.remove(player.uniqueId) else current.put(player.uniqueId, region)
        if (previous === region) return
        previous?.onLeave(player)
        region?.onEnter(player)
    }

    /** Forgets [player], calling the leave hook of their region. */
    fun remove(player: Player) {
        current.remove(player.uniqueId)?.onLeave(player)
    }
}
