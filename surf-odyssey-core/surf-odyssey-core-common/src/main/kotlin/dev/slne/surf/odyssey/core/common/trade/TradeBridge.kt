package dev.slne.surf.odyssey.core.common.trade

import dev.slne.surf.api.core.util.requiredService
import java.util.*

private val bridge = requiredService<TradeBridge>()

/** Client side access to the [TradeService] of the microservice. */
interface TradeBridge {
    /** Whether [playerUuid] accepts incoming trade requests; players without a stored setting accept them. */
    suspend fun isReceivingRequests(playerUuid: UUID): Boolean

    /** Stores whether [playerUuid] accepts incoming trade requests. */
    suspend fun setReceivingRequests(playerUuid: UUID, enabled: Boolean)

    /** Stores a finished trade with the items both players offered. */
    suspend fun saveTrade(record: TradeRecord)

    /** Stores a resolved trade request. */
    suspend fun saveRequest(record: TradeRequestRecord)

    companion object : TradeBridge by bridge {
        val INSTANCE get() = bridge
    }
}
