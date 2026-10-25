package dev.slne.surf.odyssey.runtime.paper.polartest.check

/** Collects the expectations of one test step. The step passes when no expectation failed. */
class CheckReport(val step: String) {
    private val failures = mutableListOf<String>()

    /** The number of expectations checked so far. */
    var checks = 0
        private set

    /** Whether every expectation held. */
    val passed: Boolean get() = failures.isEmpty()

    /** One message per failed expectation, in the order they were checked. */
    val failureMessages: List<String> get() = failures.toList()

    /** Fails with `<what>: erwartet <expected>, gefunden <actual>` unless both are equal. */
    fun expect(what: String, expected: Any?, actual: Any?) {
        checks++
        if (expected != actual) failures += "$what: erwartet <$expected>, gefunden <$actual>"
    }

    /** Fails with [what] unless [condition] holds. */
    fun expectTrue(what: String, condition: Boolean) {
        checks++
        if (!condition) failures += what
    }

    /** Records a failure with [message]. */
    fun fail(message: String) {
        checks++
        failures += message
    }
}
