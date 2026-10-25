package dev.slne.surf.odyssey.runtime.paper.polartest.command

import java.util.concurrent.atomic.AtomicBoolean

/** Lets one test run at a time. */
class StepGate {
    private val busy = AtomicBoolean(false)

    /** Enters the gate and returns `true`, or returns `false` when a run is already inside. */
    fun tryEnter(): Boolean = busy.compareAndSet(false, true)

    /** Leaves the gate so the next run can enter. */
    fun leave() = busy.set(false)
}
