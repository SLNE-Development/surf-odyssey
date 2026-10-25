package dev.slne.surf.odyssey.runtime.paper.polartest.data

import dev.slne.surf.odyssey.runtime.paper.polartest.layout.BlockPos
import dev.slne.surf.polarpaper.api.data.ChunkDataHandler
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.entity.Entity

/** Persists the [MarkedPositionsRegistry] entries of a chunk in its [MarkedPositions] section. */
object MarkedPositionsHandler : ChunkDataHandler<List<BlockPos>> {
    /** The section this handler reads and writes. */
    override val type = MarkedPositions

    /** Returns the registry entry of [chunk], or `null` when it has none, which stores no section. */
    override fun capture(chunk: Chunk, entities: List<Entity>, claim: (Entity) -> Unit): List<BlockPos>? =
        MarkedPositionsRegistry.get(chunk.ref())?.ifEmpty { null }

    /** Puts the stored [value] back into the registry entry of [chunk]. */
    override fun restore(chunk: Chunk, value: List<BlockPos>) = MarkedPositionsRegistry.set(chunk.ref(), value)

    /** Releases the registry entry of the chunk at [x], [z] of [world]. */
    override fun chunkUnloaded(world: World, x: Int, z: Int) = MarkedPositionsRegistry.remove(ChunkRef(world.uid, x, z))

    /** The [ChunkRef] of this chunk. */
    private fun Chunk.ref() = ChunkRef(world.uid, x, z)
}
