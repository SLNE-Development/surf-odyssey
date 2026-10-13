package dev.slne.surf.odyssey.core.common.player

import dev.slne.surf.api.core.util.requiredService
import it.unimi.dsi.fastutil.objects.ObjectSet
import org.jetbrains.annotations.UnmodifiableView
import java.util.*

private val manager = requiredService<PlayerManager>()

interface PlayerManager {
    val players: @UnmodifiableView ObjectSet<OdysseyPlayer>

    fun cachedPlayer(
        uuid: UUID
    ): OdysseyPlayer? = players.firstOrNull { it.uuid == uuid }

    fun cachedPlayerOrFail(
        uuid: UUID
    ): OdysseyPlayer = cachedPlayer(uuid) ?: error("Player with UUID $uuid is not cached")

    suspend fun findOrCreatePlayer(
        uuid: UUID,
        cache: Boolean = false
    ): OdysseyPlayer

    suspend fun findPlayer(
        uuid: UUID,
        cache: Boolean = false
    ): OdysseyPlayer?

    companion object : PlayerManager by manager {
        val INSTANCE get() = manager
    }
}