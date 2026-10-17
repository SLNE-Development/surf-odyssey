package dev.slne.surf.odyssey.runtime.paper.pack

import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperMain
import dev.slne.surf.odyssey.runtime.paper.common.OdysseyPaperServerType

/** Plugin of the pack server, which builds the resource pack with CraftEngine and uploads it; players never join it. */
class PackPaperMain : OdysseyPaperMain() {
    override val serverType = OdysseyPaperServerType.PACK
}
