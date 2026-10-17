package dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** A set of blocks that a region covers. */
sealed interface Shape {
    /** The smallest box that contains every block of this shape. */
    val bounds: Bounds

    /** Returns whether the block at [x], [y], [z] belongs to this shape. */
    fun contains(x: Int, y: Int, z: Int): Boolean
}

/** A box of blocks between two corners, both included. */
data class Cuboid(override val bounds: Bounds) : Shape {
    /** Returns whether the block at [x], [y], [z] lies inside the box. */
    override fun contains(x: Int, y: Int, z: Int): Boolean = bounds.contains(x, y, z)

    companion object {
        /** Creates the cuboid spanned by [a] and [b] in any corner order. */
        fun of(a: BlockPoint, b: BlockPoint) = Cuboid(
            Bounds(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z), max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))
        )
    }
}

/**
 * A polygon on the X/Z plane extruded from [minY] to [maxY], both included.
 *
 * A block belongs to the prism when its column lies inside the polygon or on one of its edges.
 * The polygon may be concave; its corners are block coordinates in the order they are walked.
 */
data class Prism(val points: List<Point2>, val minY: Int, val maxY: Int) : Shape {
    init {
        require(points.size >= 3) { "A prism needs at least three points" }
        require(minY <= maxY) { "minY must not be above maxY" }
    }

    /** The box around the outline, from [minY] to [maxY]. */
    override val bounds = Bounds(points.minOf { it.x }, minY, points.minOf { it.z }, points.maxOf { it.x }, maxY, points.maxOf { it.z })

    /** Returns whether the block is within the height range and its column lies inside or on the outline. */
    override fun contains(x: Int, y: Int, z: Int): Boolean = bounds.contains(x, y, z) && inside(x.toDouble(), z.toDouble())

    /** Returns whether the point lies inside the outline or on one of its edges. */
    private fun inside(px: Double, pz: Double): Boolean {
        var inside = false
        var j = points.lastIndex
        for (i in points.indices) {
            val ax = points[i].x.toDouble()
            val az = points[i].z.toDouble()
            val bx = points[j].x.toDouble()
            val bz = points[j].z.toDouble()
            if (onSegment(px, pz, ax, az, bx, bz)) return true
            if ((az > pz) != (bz > pz) && px < (bx - ax) * (pz - az) / (bz - az) + ax) inside = !inside
            j = i
        }
        return inside
    }

    /** Returns whether the point lies on the segment from a to b. */
    private fun onSegment(px: Double, pz: Double, ax: Double, az: Double, bx: Double, bz: Double): Boolean {
        val cross = (bx - ax) * (pz - az) - (bz - az) * (px - ax)
        return abs(cross) < 1e-9 && px in min(ax, bx)..max(ax, bx) && pz in min(az, bz)..max(az, bz)
    }
}

/** The blocks of all [includes] that no shape in [excludes] covers. */
data class CompoundShape(val includes: List<Shape>, val excludes: List<Shape>) : Shape {
    init {
        require(includes.isNotEmpty()) { "A shape needs at least one included part" }
    }

    /** The box around all included parts. */
    override val bounds: Bounds = includes.map { it.bounds }.reduce(Bounds::union)

    /** Returns whether an included part covers the block and no excluded part does. */
    override fun contains(x: Int, y: Int, z: Int): Boolean =
        bounds.contains(x, y, z) && includes.any { it.contains(x, y, z) } && excludes.none { it.contains(x, y, z) }
}
