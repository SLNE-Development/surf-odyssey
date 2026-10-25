package dev.slne.surf.odyssey.runtime.paper.polartest

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin

/**
 * Dev-only plugin that exercises worlds, data sections, handlers and migrations of surf-polar-paper on the home
 * dev server. It refuses to enable when the installed surf-polar-paper lacks the per-plugin API.
 */
class PolarTestPaperMain : SuspendingJavaPlugin() {
    /** Disables the plugin with [PolarApiCheck.MISSING_MESSAGE] when the per-plugin API is not installed. */
    override suspend fun onEnableAsync() {
        if (!PolarApiCheck.isNewApiPresent(javaClass.classLoader)) {
            logger.severe(PolarApiCheck.MISSING_MESSAGE)
            server.pluginManager.disablePlugin(this)
            return
        }
    }
}
