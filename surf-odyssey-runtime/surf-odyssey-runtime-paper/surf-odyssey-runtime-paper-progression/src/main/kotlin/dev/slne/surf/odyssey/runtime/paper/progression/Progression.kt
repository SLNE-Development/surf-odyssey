package dev.slne.surf.odyssey.runtime.paper.progression

import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

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

    /** The installed engine, or `null` while none is installed. */
    val runtimeOrNull: ProgressionRuntime?
        get() = if (::runtime.isInitialized) runtime else null

    /** Backs [isAvailable]. */
    private val available = AtomicBoolean(true)

    /** Whether the progression service answered the last call. */
    val isAvailable: Boolean
        get() = available.get()

    /**
     * Marks the progression service as unreachable until a later call succeeds and logs the failure. Only the first
     * failure of an outage is logged as a warning, without stack trace; every failure is logged with its stack trace
     * at [Level.FINE].
     *
     * @param logger the log to write to
     * @param action what was attempted
     * @param error the failure
     */
    internal fun reportFailure(logger: Logger, action: String, error: Throwable) {
        if (available.compareAndSet(true, false)) {
            logger.warning("The progression service is unreachable: could not $action (${error.message})")
        }
        logger.log(Level.FINE, "Could not $action", error)
    }

    /** Marks the progression service as reachable. */
    internal fun markAvailable() {
        available.set(true)
    }
}
