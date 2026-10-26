package dev.slne.surf.odyssey.runtime.paper.polartest.steps

/**
 * A step of the polar-paper test, run by `/polartest <id>`. Steps with [inRun] also run in `/polartest run`.
 *
 * @property id the literal of the step in `/polartest`
 * @property inRun whether `/polartest run` runs this step
 */
enum class PolarTestStep(val id: String, val inRun: Boolean) {
    SETUP("setup", true),
    HANDLER("handler", true),
    MIGRATE_RAW("migrate-raw", true),
    MIGRATE_LIVE("migrate-live", true),
    FURNITURE("furniture", true),
    FAIL_NEWER("fail-newer", true),
    FAIL_THROW("fail-throw", true),
    CLEANUP("cleanup", false);

    companion object {
        /** The steps of `/polartest run`, in the order they run. */
        val runOrder: List<PolarTestStep> = entries.filter { it.inRun }
    }
}
