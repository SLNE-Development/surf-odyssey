package dev.slne.surf.odyssey.runtime.paper.common

import dev.slne.surf.odyssey.runtime.paper.common.commands.home.homeCommand
import dev.slne.surf.odyssey.runtime.paper.common.commands.trade.tradeCommand

object OdysseyPaperCommandManager {
    fun registerAllCommands() {
        homeCommand()
        tradeCommand()
    }
}