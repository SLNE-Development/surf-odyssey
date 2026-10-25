package dev.slne.surf.odyssey.runtime.paper.polartest.layout

import org.bukkit.Location
import org.bukkit.World

/**
 * The fixed layout of the test worlds: a stone floor in every test chunk, three cobblestone blocks and two mossy
 * cobblestone blocks per chunk on the row above it, and one test furniture in chunk 0, 0.
 */
object TestLayout {
    /** The chunks that hold test blocks. */
    val chunks: List<Pair<Int, Int>> = listOf(0 to 0, 1 to 0, 0 to 1, -1 to -1)

    /** The y of the stone floor. */
    const val FLOOR_Y = 60

    /** The y of the cobblestone and mossy cobblestone blocks. */
    const val BLOCK_Y = 61

    /** The highest y the live migration scans. */
    const val TOP_Y = 63

    /** The cobblestone positions of the chunk at [chunkX], [chunkZ]. */
    fun cobblestone(chunkX: Int, chunkZ: Int): List<BlockPos> =
        listOf(2, 3, 4).map { BlockPos(chunkX * 16 + it, BLOCK_Y, chunkZ * 16 + 2) }

    /** The mossy cobblestone positions of the chunk at [chunkX], [chunkZ]. */
    fun mossy(chunkX: Int, chunkZ: Int): List<BlockPos> =
        listOf(2, 3).map { BlockPos(chunkX * 16 + it, BLOCK_Y, chunkZ * 16 + 6) }

    /** Where players are put to look at the test blocks. */
    fun spawn(world: World): Location = Location(world, 8.5, BLOCK_Y.toDouble(), 8.5)

    /** Where the test furniture is placed. */
    fun furniture(world: World): Location = Location(world, 8.5, BLOCK_Y.toDouble(), 12.5, 90f, 0f)
}
