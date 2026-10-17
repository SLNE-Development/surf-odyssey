package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

/** The set of mobs a spawn region currently counts as alive, identified by [K]. */
class AliveMobs<K> {
    private val alive = mutableSetOf<K>()

    /** Number of mobs counted as alive. */
    val count: Int get() = alive.size

    /** Counts the mob identified by [key] as alive. */
    fun add(key: K) {
        alive += key
    }

    /** Stops counting every mob for which [isAlive] returns false. */
    fun prune(isAlive: (K) -> Boolean) {
        alive.removeIf { !isAlive(it) }
    }

    /** Calls [remove] for every mob counted as alive, then stops counting all of them. */
    fun removeAll(remove: (K) -> Unit) {
        alive.forEach(remove)
        alive.clear()
    }
}
