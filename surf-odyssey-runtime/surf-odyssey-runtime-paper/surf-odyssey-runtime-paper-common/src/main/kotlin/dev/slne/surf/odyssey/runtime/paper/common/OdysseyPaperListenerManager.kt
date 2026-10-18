package dev.slne.surf.odyssey.runtime.paper.common

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.common.listener.*

object OdysseyPaperListenerManager {
    fun registerAllListeners() {
        PlayerConnectionListener.register()
        TradeListener.register()
    }
}


