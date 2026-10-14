package dev.slne.surf.odyssey.runtime.paper.common.commands.home

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeView
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry

/** Registers `/home`, which opens the home menu for the executing player. */
fun homeCommand() = commandAPICommand("home") {
    withPermission(OdysseyBasePermissionRegistry.HOME_COMMAND)

    playerExecutor { player, _ ->
        homeView.open(player)
    }
}
