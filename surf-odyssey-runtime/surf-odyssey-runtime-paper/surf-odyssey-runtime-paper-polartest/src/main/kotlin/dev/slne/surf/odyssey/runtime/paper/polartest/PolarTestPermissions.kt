package dev.slne.surf.odyssey.runtime.paper.polartest

import dev.slne.surf.api.paper.permission.PermissionRegistry

/** Permissions of the polar-paper test plugin. */
object PolarTestPermissions : PermissionRegistry() {
    /** Runs `/polartest` and all its steps. */
    val COMMAND = create("surf.odyssey.polartest")
}
