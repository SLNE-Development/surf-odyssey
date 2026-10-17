package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.command

import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockPos
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object Selections {
    private data class Corner(val world: UUID, val pos: BlockPos)

    private val first = ConcurrentHashMap<UUID, Corner>()
    private val second = ConcurrentHashMap<UUID, Corner>()

    fun setFirst(player: Player) {
        first[player.uniqueId] = player.corner()
    }

    fun setSecond(player: Player) {
        second[player.uniqueId] = player.corner()
    }

    fun setFirst(player: Player, pos: BlockPos) {
        first[player.uniqueId] = Corner(player.world.uid, pos)
    }

    fun setSecond(player: Player, pos: BlockPos) {
        second[player.uniqueId] = Corner(player.world.uid, pos)
    }

    fun peek(player: Player): Pair<BlockPos?, BlockPos?> {
        val world = player.world.uid
        return first[player.uniqueId]?.takeIf { it.world == world }?.pos to second[player.uniqueId]?.takeIf { it.world == world }?.pos
    }

    fun corners(player: Player): Pair<BlockPos, BlockPos>? {
        val a = first[player.uniqueId] ?: return null
        val b = second[player.uniqueId] ?: return null
        if (a.world != player.world.uid || b.world != player.world.uid) return null

        return a.pos to b.pos
    }

    fun forget(uuid: UUID) {
        first.remove(uuid)
        second.remove(uuid)
    }

    private fun Player.corner() = Corner(world.uid, BlockPos(location.blockX, location.blockY, location.blockZ))
}
