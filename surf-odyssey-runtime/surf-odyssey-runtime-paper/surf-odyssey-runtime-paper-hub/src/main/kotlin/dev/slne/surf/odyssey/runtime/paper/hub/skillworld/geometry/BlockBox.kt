package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class BlockBox(
    val minX: Int, val minY: Int, val minZ: Int,
    val maxX: Int, val maxY: Int, val maxZ: Int,
) {
    val volume: Long get() = (maxX - minX + 1L) * (maxY - minY + 1L) * (maxZ - minZ + 1L)

    fun contains(x: Int, y: Int, z: Int): Boolean = x in minX..maxX && y in minY..maxY && z in minZ..maxZ

    fun contains(x: Double, y: Double, z: Double): Boolean =
        contains(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())

    companion object {
        fun of(a: BlockPos, b: BlockPos) = BlockBox(
            min(a.x, b.x), min(a.y, b.y), min(a.z, b.z),
            max(a.x, b.x), max(a.y, b.y), max(a.z, b.z),
        )
    }
}
