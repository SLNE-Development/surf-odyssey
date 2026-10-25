package dev.slne.surf.odyssey.runtime.paper.polartest

/** Detects whether the installed surf-polar-paper provides the per-plugin API this plugin is built against. */
object PolarApiCheck {
    /** A class that only exists in the per-plugin API. */
    const val FACTORY_CLASS = "dev.slne.surf.polarpaper.api.SurfPolarPaperApiFactory"

    /** Logged when the installed runtime is too old. */
    const val MISSING_MESSAGE = "surf-polar-paper-runtime-paper is too old: $FACTORY_CLASS is missing. " +
            "Replace the jar with the current 1.0.1 release asset of surf-polar-paper."

    /**
     * Returns whether [loader] can see [FACTORY_CLASS], without initializing it.
     *
     * @param loader the class loader to look the class up in
     * @return `true` when the per-plugin API is present
     */
    fun isNewApiPresent(loader: ClassLoader): Boolean = try {
        Class.forName(FACTORY_CLASS, false, loader)
        true
    } catch (_: ClassNotFoundException) {
        false
    }
}
