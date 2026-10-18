package dev.slne.surf.odyssey.runtime.paper.progression

/**
 * Global access to the progression engine of this server.
 *
 * [isAvailable] turns `false` when a call to the progression service fails and turns `true` again once the service
 * answers. Features that need the service check it and tell the player that they are not available.
 */
object Progression {
    /** The engine installed by [ProgressionModule.install]. */
    lateinit var runtime: ProgressionRuntime
        internal set

    /** Whether the progression service answered the last call. */
    @Volatile
    var isAvailable: Boolean = true
        internal set

    /** Marks the progression service as unreachable until a later call succeeds. */
    internal fun markUnavailable() {
        isAvailable = false
    }

    /** Marks the progression service as reachable. */
    internal fun markAvailable() {
        isAvailable = true
    }
}
