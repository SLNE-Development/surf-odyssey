package dev.slne.surf.odyssey.core.common.home

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.rabbitmq.api.rpc.RpcService

@RpcService
interface HomeService {
    suspend fun listHomes(playerUuid: SerializableUUID): List<Home>

    suspend fun createHome(playerUuid: SerializableUUID, home: Home)
    suspend fun saveHome(playerUuid: SerializableUUID, home: Home)
    suspend fun deleteHome(playerUuid: SerializableUUID, home: Home)
}