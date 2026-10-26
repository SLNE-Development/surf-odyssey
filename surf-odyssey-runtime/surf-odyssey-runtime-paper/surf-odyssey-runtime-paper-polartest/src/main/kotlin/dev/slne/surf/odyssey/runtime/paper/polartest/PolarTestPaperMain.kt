package dev.slne.surf.odyssey.runtime.paper.polartest

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.odyssey.runtime.paper.polartest.command.StepRunner
import dev.slne.surf.odyssey.runtime.paper.polartest.command.polarTestCommand
import dev.slne.surf.odyssey.runtime.paper.polartest.session.PolarTestSession
import dev.slne.surf.odyssey.runtime.paper.polartest.steps.PolarTestSteps

/**
 * Dev-only plugin that exercises worlds, data sections, handlers and migrations of surf-polar-paper on the home
 * dev server through `/polartest`. It refuses to enable when the installed surf-polar-paper lacks the per-plugin API.
 */
class PolarTestPaperMain : SuspendingJavaPlugin() {
    private var session: PolarTestSession? = null

    /** Checks the API, then registers `/polartest`. */
    override suspend fun onEnableAsync() {
        if (!PolarApiCheck.isNewApiPresent(javaClass.classLoader)) {
            logger.severe(PolarApiCheck.MISSING_MESSAGE)
            server.pluginManager.disablePlugin(this)
            return
        }

        val created = PolarTestSession(this)
        session = created
        polarTestCommand(StepRunner(this, PolarTestSteps(this, created)))
    }

    /**
     * Moves players out of the test worlds, then saves, unloads and releases them when the plugin is disabled while
     * the server keeps running. While the server stops, surf-polar-paper saves the worlds of this plugin by itself.
     */
    override suspend fun onDisableAsync() {
        if (server.isStopping) return
        session?.closeApi()
    }
}
