package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

/**
 * Runs the portal transfer [send] and calls [cancelDeparture] unless the transfer succeeded.
 *
 * The departure is cancelled when [send] returns `false` and also when it throws, including when the coroutine is
 * cancelled; the error is passed on afterwards.
 */
internal suspend fun sendOrCancelDeparture(send: suspend () -> Boolean, cancelDeparture: () -> Unit) {
    val sent = try {
        send()
    } catch (throwable: Throwable) {
        cancelDeparture()
        throw throwable
    }
    if (!sent) cancelDeparture()
}
