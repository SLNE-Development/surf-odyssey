package dev.slne.surf.odyssey.core.client

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.rabbitmq.api.ClientRabbitMQApi
import java.nio.file.Path

private val instance = requiredService<OdysseyClientInstance>()

abstract class OdysseyClientInstance {
    abstract val dataPath: Path

    val rabbitApi = ClientRabbitMQApi.create("surf-odyssey", dataPath)

    suspend fun onLoad() {

    }

    suspend fun onEnable() {

    }

    suspend fun onDisable() {

    }

    companion object {
        val INSTANCE get() = instance
    }
}