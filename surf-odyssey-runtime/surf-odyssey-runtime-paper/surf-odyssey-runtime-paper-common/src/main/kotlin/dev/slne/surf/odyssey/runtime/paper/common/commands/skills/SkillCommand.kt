package dev.slne.surf.odyssey.runtime.paper.common.commands.skills

import dev.jorel.commandapi.kotlindsl.commandTree
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry

fun skillCommand() = commandTree("skills") {
    withPermission(OdysseyBasePermissionRegistry.SKILL_COMMAND)
}