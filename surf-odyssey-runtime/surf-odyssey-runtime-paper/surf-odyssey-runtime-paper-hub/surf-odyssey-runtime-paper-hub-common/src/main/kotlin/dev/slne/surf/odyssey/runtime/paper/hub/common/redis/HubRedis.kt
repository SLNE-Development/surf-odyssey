package dev.slne.surf.odyssey.runtime.paper.hub.common.redis

import dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect.ReconnectCache
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.ArrivalCache
import dev.slne.surf.redis.RedisApi

/**
 * The single Redis connection of a hub server, shared by the [ArrivalCache] and the [ReconnectCache].
 */
object HubRedis {
    private lateinit var redis: RedisApi

    /** Creates the connection for the plugin named [pluginName], attaches both caches to it and connects it. */
    fun connect(pluginName: String) {
        redis = RedisApi.create(pluginName)
        ArrivalCache.attach(redis)
        ReconnectCache.attach(redis)
        redis.freezeAndConnect()
    }

    /** Disconnects from Redis if [connect] was called. */
    fun disconnect() {
        if (::redis.isInitialized) redis.disconnect()
    }
}
