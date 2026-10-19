package dev.slne.surf.odyssey.core.client.scoreboard

import com.google.auto.service.AutoService
import dev.slne.surf.odyssey.core.client.OdysseyClientInstance
import dev.slne.surf.odyssey.core.common.scoreboard.ScoreboardBridge
import dev.slne.surf.odyssey.core.common.scoreboard.ScoreboardService
import java.util.*

private val proxy by lazy {
    OdysseyClientInstance.INSTANCE.rabbitApi.createRpcService<ScoreboardService>()
}

/** Forwards every scoreboard setting call to the microservice over RabbitMQ. */
@AutoService(ScoreboardBridge::class)
class ScoreboardBridgeImpl : ScoreboardBridge {
    override suspend fun isEnabled(playerUuid: UUID): Boolean {
        return proxy.isEnabled(playerUuid)
    }

    override suspend fun setEnabled(playerUuid: UUID, enabled: Boolean) {
        proxy.setEnabled(playerUuid, enabled)
    }
}
