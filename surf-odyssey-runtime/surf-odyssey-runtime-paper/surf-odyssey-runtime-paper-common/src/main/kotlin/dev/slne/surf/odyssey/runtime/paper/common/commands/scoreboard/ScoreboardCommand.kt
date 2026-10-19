package dev.slne.surf.odyssey.runtime.paper.common.commands.scoreboard

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import dev.slne.surf.odyssey.runtime.paper.common.scoreboard.OdysseyScoreboard
import dev.slne.surf.odyssey.runtime.paper.common.scoreboard.scoreboardPreferences
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry
import java.util.logging.Level

fun scoreboardCommand() = commandTree("scoreboard") {
    withPermission(OdysseyBasePermissionRegistry.SCOREBOARD_COMMAND)

    playerExecutor { player, _ ->
        plugin.launch {
            val enabled = scoreboardPreferences.toggle(player.uniqueId)

            player.scheduler.run(plugin, {
                if (scoreboardPreferences.isEnabled(player.uniqueId)) OdysseyScoreboard.show(player) else OdysseyScoreboard.hide(player)
            }, null)

            player.sendText {
                appendInfoPrefix()
                info(if (enabled) "Das Scoreboard ist jetzt eingeschaltet." else "Das Scoreboard ist jetzt ausgeschaltet.")
            }

            scoreboardPreferences.persist(player.uniqueId)?.let {
                plugin.logger.log(Level.SEVERE, "Failed to store the scoreboard setting of ${player.name}", it)
                player.sendText {
                    appendErrorPrefix()
                    error("Die Einstellung konnte nicht gespeichert werden und gilt nur bis zum Logout.")
                }
            }
        }
    }
}
