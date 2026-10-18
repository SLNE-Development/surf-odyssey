package dev.slne.surf.odyssey.runtime.paper.hub.common

import dev.slne.surf.api.paper.permission.PermissionRegistry

/** Permissions shared by all hub worlds. */
object HubPermissions : PermissionRegistry() {
    private const val PREFIX = "surf.odyssey.hub"

    /** Allows using the region editor. */
    val REGION_EDITOR = create("$PREFIX.region.editor")

    /** Exempts from region, portal and world gates. */
    val GATE_BYPASS = create("surf.odyssey.progression.gate.bypass")
}
