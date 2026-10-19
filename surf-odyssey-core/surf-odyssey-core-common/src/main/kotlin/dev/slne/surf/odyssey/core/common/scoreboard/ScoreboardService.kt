package dev.slne.surf.odyssey.core.common.scoreboard

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.rabbitmq.api.rpc.RpcService

/** Stores, per player, whether the scoreboard is shown. */
@RpcService
interface ScoreboardService {
    /** Whether [playerUuid] sees the scoreboard; players without a stored setting see it. */
    suspend fun isEnabled(playerUuid: SerializableUUID): Boolean

    /** Stores whether [playerUuid] sees the scoreboard. */
    suspend fun setEnabled(playerUuid: SerializableUUID, enabled: Boolean)
}
