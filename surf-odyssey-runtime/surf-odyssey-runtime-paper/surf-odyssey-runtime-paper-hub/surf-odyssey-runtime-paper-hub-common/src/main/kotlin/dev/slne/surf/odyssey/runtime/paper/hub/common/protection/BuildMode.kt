package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Players who build freely, ignoring region rules. Cleared when they leave. */
object BuildMode {
    private val active: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /** Returns whether [player] is in build mode. */
    fun isActive(player: Player): Boolean = player.uniqueId in active

    /** Switches build mode for [player] and returns the new state. */
    fun toggle(player: Player): Boolean = if (active.remove(player.uniqueId)) false else active.add(player.uniqueId)

    /** Ends build mode for the player with [uuid]. */
    fun clear(uuid: UUID) {
        active.remove(uuid)
    }
}
