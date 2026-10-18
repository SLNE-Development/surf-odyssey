package dev.slne.surf.odyssey.runtime.paper.hub.common

/**
 * Runs shutdown steps one after another so that a failing step never skips the steps after it.
 * Failures are collected and thrown by [rethrow] once every step has run.
 */
class ShutdownSteps {
    /** The first failure, with every later one added as suppressed; `null` while every step succeeded. */
    @PublishedApi
    internal var failure: Exception? = null

    /** Runs [step] and records the exception it throws instead of passing it on. */
    inline fun run(step: () -> Unit) {
        try {
            step()
        } catch (e: Exception) {
            val first = failure
            if (first == null) failure = e else first.addSuppressed(e)
        }
    }

    /** Throws the first recorded failure, with the later ones suppressed; does nothing if every step succeeded. */
    fun rethrow() {
        failure?.let { throw it }
    }
}
