package dev.slne.surf.odyssey.core.common.player

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.rabbitmq.api.rpc.RpcService

@RpcService
interface PlayerService {
    suspend fun findOrCreatePlayer(uuid: SerializableUUID): OdysseyPlayerDto
    suspend fun findPlayer(uuid: SerializableUUID): OdysseyPlayerDto?
}