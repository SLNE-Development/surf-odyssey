package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.cache.SimpleRedisCache
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Arrival tickets shared by all hub servers through Redis.
 *
 * A ticket lives for five seconds, long enough for the transfer to finish.
 */
object ArrivalCache {
    private lateinit var tickets: SimpleRedisCache<UUID, ArrivalTicket>

    /** Creates the cache on [redis], which must not be connected yet. */
    fun attach(redis: RedisApi) {
        tickets = redis.createSimpleCache("odyssey:hub-arrival", 5.seconds)
    }

    /** Stores [ticket] for [player], replacing an earlier one. */
    suspend fun put(player: UUID, ticket: ArrivalTicket) = tickets.put(player, ticket)

    /** Returns the ticket of [player], or `null` if there is none. */
    suspend fun get(player: UUID): ArrivalTicket? = tickets.getCached(player)

    /** Removes the ticket of [player]. */
    suspend fun invalidate(player: UUID) {
        tickets.invalidate(player)
    }
}
