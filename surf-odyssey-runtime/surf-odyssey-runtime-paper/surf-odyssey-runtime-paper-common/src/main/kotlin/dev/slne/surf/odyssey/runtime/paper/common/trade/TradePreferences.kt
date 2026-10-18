package dev.slne.surf.odyssey.runtime.paper.common.trade

import dev.slne.surf.odyssey.core.common.trade.TradeBridge
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Caches, for every online player, whether they accept incoming trade requests. */
internal object TradePreferences {
    private val blocking = ConcurrentHashMap.newKeySet<UUID>()

    /** Whether [uuid] accepts incoming trade requests; players that are not cached accept them. */
    fun isReceiving(uuid: UUID) = uuid !in blocking

    /** Loads the stored setting of [uuid] into the cache. */
    suspend fun load(uuid: UUID) {
        cache(uuid, TradeBridge.isReceivingRequests(uuid))
    }

    /** Stores whether [uuid] accepts incoming trade requests and applies it to the cache once it is stored. */
    suspend fun setReceiving(uuid: UUID, enabled: Boolean) {
        TradeBridge.setReceivingRequests(uuid, enabled)
        cache(uuid, enabled)
    }

    /** Removes [uuid] from the cache. */
    fun evict(uuid: UUID) {
        blocking.remove(uuid)
    }

    private fun cache(uuid: UUID, enabled: Boolean) {
        if (enabled) blocking.remove(uuid) else blocking.add(uuid)
    }
}
