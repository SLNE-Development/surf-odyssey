package dev.slne.surf.odyssey.core.client.trade

import com.google.auto.service.AutoService
import dev.slne.surf.odyssey.core.client.OdysseyClientInstance
import dev.slne.surf.odyssey.core.common.trade.TradeBridge
import dev.slne.surf.odyssey.core.common.trade.TradeRecord
import dev.slne.surf.odyssey.core.common.trade.TradeRequestRecord
import dev.slne.surf.odyssey.core.common.trade.TradeService
import java.util.*

private val proxy by lazy {
    OdysseyClientInstance.INSTANCE.rabbitApi.createRpcService<TradeService>()
}

/** Forwards every trade call to the microservice over RabbitMQ. */
@AutoService(TradeBridge::class)
class TradeBridgeImpl : TradeBridge {
    override suspend fun isReceivingRequests(playerUuid: UUID): Boolean {
        return proxy.isReceivingRequests(playerUuid)
    }

    override suspend fun setReceivingRequests(playerUuid: UUID, enabled: Boolean) {
        proxy.setReceivingRequests(playerUuid, enabled)
    }

    override suspend fun saveTrade(record: TradeRecord) {
        proxy.saveTrade(record)
    }

    override suspend fun saveRequest(record: TradeRequestRecord) {
        proxy.saveRequest(record)
    }
}
