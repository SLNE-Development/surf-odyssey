package dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect

import dev.slne.surf.odyssey.core.common.reconnect.ReconnectEntry
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.libs.redisson.api.RBucket
import dev.slne.surf.redis.libs.redisson.client.codec.StringCodec
import kotlinx.coroutines.future.await
import java.util.UUID

/**
 * Reconnect entries shared by all hub servers and the proxy through Redis.
 *
 * Each player has at most one entry, stored as JSON without expiry and replaced on every write.
 */
object ReconnectCache {
    private lateinit var redis: RedisApi

    /** Stores and reads the entries through [redis]. */
    fun attach(redis: RedisApi) {
        this.redis = redis
    }

    /** Stores [entry] for [player] without expiry, replacing an earlier one. */
    suspend fun put(player: UUID, entry: ReconnectEntry) {
        bucket(player).setAsync(redis.json.encodeToString(ReconnectEntry.serializer(), entry)).await()
    }

    /** Returns the entry of [player], or `null` if there is none. */
    suspend fun get(player: UUID): ReconnectEntry? =
        bucket(player).getAsync().await()?.let { redis.json.decodeFromString(ReconnectEntry.serializer(), it) }

    /** Returns the Redis bucket holding the entry of [player] as a string. */
    private fun bucket(player: UUID): RBucket<String> =
        redis.redisson.getBucket(ReconnectEntry.redisKey(player), StringCodec.INSTANCE)
}
