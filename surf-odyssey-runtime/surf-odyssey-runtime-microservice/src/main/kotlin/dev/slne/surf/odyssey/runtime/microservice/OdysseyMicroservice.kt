package dev.slne.surf.odyssey.runtime.microservice

import com.google.auto.service.AutoService
import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.microservice.api.microservice.Microservice
import dev.slne.surf.rabbitmq.api.ServerRabbitMQApi
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Serves the odyssey RPC services to the Paper and Velocity plugins over RabbitMQ and stores their data in the
 * database.
 */
@AutoService(Microservice::class)
class OdysseyMicroservice : Microservice() {
    override val dataPath: Path = Path("config")

    private val databaseApi = DatabaseApi.create(dataPath)
    private val rabbitApi = ServerRabbitMQApi.create("surf-odyssey", dataPath)

    override suspend fun onBootstrap(args: List<String>) {
        rabbitApi.freezeAndConnect()
    }

    override suspend fun onDisable() {
        rabbitApi.disconnect()
        databaseApi.shutdown()
    }
}
