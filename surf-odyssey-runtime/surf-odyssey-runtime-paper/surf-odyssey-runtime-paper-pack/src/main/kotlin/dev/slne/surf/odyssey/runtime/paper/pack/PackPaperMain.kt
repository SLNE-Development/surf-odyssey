package dev.slne.surf.odyssey.runtime.paper.pack

import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperMain
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperServerType

/** Plugin of the pack server, which builds the resource pack with CraftEngine and uploads it; players never join it. */
class PackPaperMain : OdysseyPaperMain() {
    override val serverType = OdysseyPaperServerType.PACK

    /** Registers the [PackBuildTrigger] that builds and uploads the pack once the server has started. */
    override suspend fun onEnabled() {
        server.pluginManager.registerEvents(PackBuildTrigger { server.dispatchCommand(server.consoleSender, it) }, this)
    }
}
