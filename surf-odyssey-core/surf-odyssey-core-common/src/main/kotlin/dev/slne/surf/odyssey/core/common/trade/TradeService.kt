package dev.slne.surf.odyssey.core.common.trade

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.rabbitmq.api.rpc.RpcService

/** Stores trade history and trade settings in the database through the microservice. */
@RpcService
interface TradeService {
    /** Whether [playerUuid] accepts incoming trade requests; players without a stored setting accept them. */
    suspend fun isReceivingRequests(playerUuid: SerializableUUID): Boolean

    /** Stores whether [playerUuid] accepts incoming trade requests. */
    suspend fun setReceivingRequests(playerUuid: SerializableUUID, enabled: Boolean)

    /** Stores a finished trade with the items both players offered. Saving the same trade twice changes nothing. */
    suspend fun saveTrade(record: TradeRecord)

    /** The newest [limit] trades [playerUuid] took part in, newest first, each with the items of both players. */
    suspend fun listTrades(playerUuid: SerializableUUID, limit: Int): List<TradeRecord>

    /** Stores a resolved trade request. Saving the same request twice changes nothing. */
    suspend fun saveRequest(record: TradeRequestRecord)
}
