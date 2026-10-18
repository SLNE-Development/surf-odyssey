package dev.slne.surf.odyssey.runtime.paper.common.utils

import dev.slne.surf.api.paper.permission.PermissionRegistry

object OdysseyBasePermissionRegistry : PermissionRegistry() {
    private const val PREFIX = "surf.odyssey"
    private const val COMMAND_PREFIX = "$PREFIX.command"

    private const val HOME_BASE = "$PREFIX.home"
    private const val HOME_COMMAND_BASE = "$COMMAND_PREFIX.home"

    // Home
    val HOME_COMMAND = create("$HOME_COMMAND_BASE.use")
    val HOME_CREATE = create("$HOME_BASE.create")
    val HOME_DELETE = create("$HOME_BASE.delete")
    val HOME_LIST = create("$HOME_BASE.list")
    val HOME_EDIT = create("$HOME_BASE.edit")

    // Hub
    val HUB_DOUBLE_JUMP = create("$PREFIX.hub.doublejump")
}