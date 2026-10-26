package dev.slne.surf.odyssey.runtime.paper.polartest.data

import dev.slne.surf.odyssey.runtime.paper.polartest.layout.BlockPos
import dev.slne.surf.polarpaper.api.data.DataType
import kotlinx.serialization.builtins.ListSerializer
import net.kyori.adventure.key.Key
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** The chunk section that holds the marked positions of a chunk. */
val MarkedPositions = DataType(Key.key("polartest", "marked-positions"), ListSerializer(BlockPos.serializer()))

/** Identifies a chunk by the uid of its Bukkit world and its chunk coordinates. */
data class ChunkRef(val worldId: UUID, val x: Int, val z: Int)

/** The marked positions of every loaded test chunk. [MarkedPositionsHandler] captures from it and restores into it. */
object MarkedPositionsRegistry {
    private val byChunk = ConcurrentHashMap<ChunkRef, List<BlockPos>>()

    /** Replaces the marked positions of [ref] with a copy of [positions]. */
    fun set(ref: ChunkRef, positions: List<BlockPos>) {
        byChunk[ref] = positions.toList()
    }

    /** The marked positions of [ref], or `null` when none are known. */
    fun get(ref: ChunkRef): List<BlockPos>? = byChunk[ref]

    /** Forgets the marked positions of [ref]. */
    fun remove(ref: ChunkRef) {
        byChunk.remove(ref)
    }

    /** Forgets the marked positions of every chunk. */
    fun clear() = byChunk.clear()
}
