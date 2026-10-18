package dev.slne.surf.odyssey.runtime.paper.progression

import dev.slne.surf.api.paper.permission.PermissionRegistry

/** Permissions of the progression engine. */
object ProgressionPermissions : PermissionRegistry() {
    /** The prefix of every progression permission. */
    private const val PREFIX = "surf.odyssey.progression"

    /** Administers progression and receives reports about suspected duplicated tools. */
    val ADMIN = create("$PREFIX.admin")
}
