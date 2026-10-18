package dev.slne.surf.odyssey.runtime.velocity.lobby

import dev.slne.surf.odyssey.core.common.reconnect.ReconnectEntry
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.libs.redisson.client.codec.StringCodec
import kotlinx.coroutines.future.await
import java.util.UUID

/** Reads and removes the reconnect entries the hub servers store in Redis. */
object ReconnectLookup {
    private lateinit var redis: RedisApi

    /** Connects to Redis for the plugin named [pluginName]. */
    fun connect(pluginName: String) {
        redis = RedisApi.create(pluginName)
        redis.freezeAndConnect()
    }

    /** Disconnects from Redis if [connect] was called. */
    fun disconnect() {
        if (::redis.isInitialized) redis.disconnect()
    }

    /** Returns the entry of [player], or `null` if there is none. */
    suspend fun get(player: UUID): ReconnectEntry? =
        redis.redisson.getBucket<String>(ReconnectEntry.redisKey(player), StringCodec.INSTANCE).getAsync().await()
            ?.let { redis.json.decodeFromString(ReconnectEntry.serializer(), it) }

    /** Removes the entry of [player] if there is one. */
    suspend fun delete(player: UUID) {
        redis.redisson.getBucket<String>(ReconnectEntry.redisKey(player), StringCodec.INSTANCE).deleteAsync().await()
    }
}
