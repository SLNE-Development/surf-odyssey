package dev.slne.surf.odyssey.runtime.paper.hub.common

import dev.slne.surf.api.paper.permission.PermissionRegistry

/** Permissions shared by all hub worlds. */
object HubPermissions : PermissionRegistry() {
    private const val PREFIX = "surf.odyssey.hub"

    /** Allows toggling build mode, which lifts all block protection for the player. */
    val BUILD_MODE = create("$PREFIX.build")

    /** Allows using the region editor. */
    val REGION_EDITOR = create("$PREFIX.region.editor")
}
