package dev.slne.surf.odyssey.runtime.paper.common.player

import dev.slne.surf.odyssey.core.common.player.PlayerManager
import org.bukkit.entity.Player

suspend fun Player.odysseyPlayer(cache: Boolean = false) =
    PlayerManager.findOrCreatePlayer(uniqueId, cache)

fun Player.cachedPlayer() = PlayerManager.cachedPlayer(uniqueId)
fun Player.cachedPlayerOrFail() = PlayerManager.cachedPlayerOrFail(uniqueId)