package dev.slne.surf.odyssey.runtime.paper.common

import com.google.auto.service.AutoService
import dev.slne.surf.odyssey.core.client.OdysseyClientInstance
import java.nio.file.Path

@AutoService(OdysseyClientInstance::class)
class OdysseyPaperClientInstance : OdysseyClientInstance() {
    override val dataPath: Path get() = plugin.dataPath
}