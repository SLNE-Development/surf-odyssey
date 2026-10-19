package dev.slne.surf.odyssey.core.common.scoreboard

import dev.slne.surf.api.core.util.requiredService
import java.util.*

private val bridge = requiredService<ScoreboardBridge>()

/** Client side access to the [ScoreboardService] of the microservice. */
interface ScoreboardBridge {
    /** Whether [playerUuid] sees the scoreboard; players without a stored setting see it. */
    suspend fun isEnabled(playerUuid: UUID): Boolean

    /** Stores whether [playerUuid] sees the scoreboard. */
    suspend fun setEnabled(playerUuid: UUID, enabled: Boolean)

    companion object : ScoreboardBridge by bridge {
        val INSTANCE get() = bridge
    }
}
