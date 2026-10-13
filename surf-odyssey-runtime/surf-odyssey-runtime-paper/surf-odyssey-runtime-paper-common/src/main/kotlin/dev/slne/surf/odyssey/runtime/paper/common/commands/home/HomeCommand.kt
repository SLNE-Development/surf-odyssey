package dev.slne.surf.odyssey.runtime.paper.common.commands.home

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry

fun homeCommand() = commandAPICommand("home") {
    withPermission(OdysseyBasePermissionRegistry.HOME_COMMAND)
}