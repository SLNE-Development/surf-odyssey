package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining

import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockBox
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockPos
import kotlinx.coroutines.delay
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import kotlin.time.Duration.Companion.milliseconds

class MiningSnapshot(val box: BlockBox, private val data: Array<BlockData>) {
    private val sizeY = box.maxY - box.minY + 1
    private val sizeZ = box.maxZ - box.minZ + 1

    fun materials(): List<Material> = data.map { it.material }

    fun positionAt(index: Int): BlockPos {
        val x = index / (sizeY * sizeZ)
        val rest = index % (sizeY * sizeZ)
        return BlockPos(box.minX + x, box.minY + rest / sizeZ, box.minZ + rest % sizeZ)
    }

    suspend fun restore(world: World, blocksPerTick: Int, shouldAbort: () -> Boolean): Boolean =
        restoreInBatches(data.size, blocksPerTick, shouldAbort) { index ->
            val pos = positionAt(index)
            world.getBlockAt(pos.x, pos.y, pos.z).setBlockData(data[index], false)
        }

    companion object {
        fun capture(world: World, box: BlockBox): MiningSnapshot {
            require(box.volume <= Int.MAX_VALUE) {
                "The mining region has ${box.volume} blocks, which is more than a snapshot can hold"
            }
            val blocks = ArrayList<BlockData>(box.volume.toInt())
            for (x in box.minX..box.maxX) for (y in box.minY..box.maxY) for (z in box.minZ..box.maxZ) {
                blocks += world.getBlockAt(x, y, z).blockData
            }
            return MiningSnapshot(box, blocks.toTypedArray())
        }
    }
}

internal suspend fun restoreInBatches(size: Int, batchSize: Int, shouldAbort: () -> Boolean, write: (Int) -> Unit): Boolean {
    val batch = batchSize.coerceAtLeast(1)
    for (start in 0 until size step batch) {
        if (shouldAbort()) return false
        for (index in start until minOf(start + batch, size)) write(index)
        delay(50.milliseconds)
    }
    return true
}
