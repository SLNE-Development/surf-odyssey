package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPermissions

/** Registers `/hubbuild`, which toggles build mode for the executing player. */
fun buildModeCommand() = commandAPICommand("hubbuild") {
    withPermission(HubPermissions.BUILD_MODE)
    playerExecutor { player, _ ->
        val active = BuildMode.toggle(player)
        player.sendText {
            appendSuccessPrefix()
            success(if (active) "Baumodus aktiviert." else "Baumodus deaktiviert.")
        }
    }
}
