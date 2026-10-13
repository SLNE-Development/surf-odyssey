package dev.slne.surf.odyssey.core.client.player

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.toObjectSet
import dev.slne.surf.odyssey.core.client.OdysseyClientInstance
import dev.slne.surf.odyssey.core.common.player.OdysseyPlayer
import dev.slne.surf.odyssey.core.common.player.PlayerManager
import dev.slne.surf.odyssey.core.common.player.PlayerService
import it.unimi.dsi.fastutil.objects.ObjectSet
import org.jetbrains.annotations.UnmodifiableView
import java.util.*

private val proxy by lazy {
    OdysseyClientInstance.INSTANCE.rabbitApi.createRpcService<PlayerService>()
}

@AutoService(PlayerManager::class)
class PlayerManagerImpl : PlayerManager {
    private val playerCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .build<UUID, OdysseyPlayer>()

    override val players: @UnmodifiableView ObjectSet<OdysseyPlayer>
        get() = playerCache.asMap().values.toObjectSet()

    override suspend fun findOrCreatePlayer(
        uuid: UUID,
        cache: Boolean
    ): OdysseyPlayer {
        val cacheHit = playerCache.getIfPresent(uuid)

        if (cacheHit != null) {
            return cacheHit
        }

        val dto = proxy.findOrCreatePlayer(uuid)

        return OdysseyPlayer.fromDto(dto).also {
            if (cache) {
                playerCache.put(uuid, it)
            }
        }
    }

    override suspend fun findPlayer(
        uuid: UUID,
        cache: Boolean
    ): OdysseyPlayer? {
        val cacheHit = playerCache.getIfPresent(uuid)

        if (cacheHit != null) {
            return cacheHit
        }

        val dto = proxy.findPlayer(uuid) ?: return null

        return OdysseyPlayer.fromDto(dto).also {
            if (cache) {
                playerCache.put(uuid, it)
            }
        }
    }
}