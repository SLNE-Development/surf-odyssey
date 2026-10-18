package dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape

import kotlin.math.max
import kotlin.math.min

/** An axis-aligned block box, inclusive on every side. */
data class Bounds(val minX: Int, val minY: Int, val minZ: Int, val maxX: Int, val maxY: Int, val maxZ: Int) {
    /** Number of blocks inside these bounds. */
    val volume: Long get() = (maxX - minX + 1L) * (maxY - minY + 1L) * (maxZ - minZ + 1L)

    /** Returns whether the block at [x], [y], [z] lies inside these bounds. */
    fun contains(x: Int, y: Int, z: Int): Boolean = x in minX..maxX && y in minY..maxY && z in minZ..maxZ

    /** Returns whether these bounds share at least one block with [other]. */
    fun intersects(other: Bounds): Boolean =
        minX <= other.maxX && maxX >= other.minX && minY <= other.maxY && maxY >= other.minY && minZ <= other.maxZ && maxZ >= other.minZ

    /** Returns the blocks both bounds share, or `null` if they share none. */
    fun intersection(other: Bounds): Bounds? = if (!intersects(other)) null else Bounds(
        max(minX, other.minX), max(minY, other.minY), max(minZ, other.minZ),
        min(maxX, other.maxX), min(maxY, other.maxY), min(maxZ, other.maxZ),
    )

    /** Returns the smallest bounds that contain both these bounds and [other]. */
    fun union(other: Bounds): Bounds = Bounds(
        min(minX, other.minX), min(minY, other.minY), min(minZ, other.minZ),
        max(maxX, other.maxX), max(maxY, other.maxY), max(maxZ, other.maxZ),
    )
}
