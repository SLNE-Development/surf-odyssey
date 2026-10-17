package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Point2

/** One part of a region being drawn; [excluded] parts are cut out of the region. */
sealed interface EditorPart {
    /** Whether this part is removed from the region instead of added. */
    val excluded: Boolean
}

/**
 * A prism part whose outline grows with every point.
 *
 * @property minY lowest block layer of the prism
 * @property maxY highest block layer of the prism
 * @property points corners of the outline in the order they were added
 * @property excluded whether this part is cut out of the region
 */
data class PrismPart(val minY: Int, val maxY: Int, val points: MutableList<Point2>, override val excluded: Boolean) : EditorPart

/**
 * A cuboid part spanned by up to two corners.
 *
 * @property corners corners added so far, at most two
 * @property excluded whether this part is cut out of the region
 */
data class CuboidPart(val corners: MutableList<BlockPoint>, override val excluded: Boolean) : EditorPart

/**
 * A region an admin is drawing: parts in the order they were started, points added to the newest.
 *
 * @property regionId id of the region being drawn
 */
class EditorSession(val regionId: String) {
    /** Parts drawn so far. */
    val parts = mutableListOf<EditorPart>()

    /** Starts a new prism part between [minY] and [maxY]. */
    fun startPrism(minY: Int, maxY: Int, excluded: Boolean) {
        parts += PrismPart(minOf(minY, maxY), maxOf(minY, maxY), mutableListOf(), excluded)
    }

    /** Starts a new cuboid part. */
    fun startCuboid(excluded: Boolean) {
        parts += CuboidPart(mutableListOf(), excluded)
    }

    /**
     * Adds [point] to the newest part. A prism keeps only its x and z.
     *
     * @return `false` if there is no part or the cuboid already has both corners
     */
    fun addPoint(point: BlockPoint): Boolean = when (val part = parts.lastOrNull()) {
        is PrismPart -> part.points.add(Point2(point.x, point.z))
        is CuboidPart -> if (part.corners.size < 2) part.corners.add(point) else false
        null -> false
    }

    /** Removes the newest point, or the newest part when it has no points; returns whether anything changed. */
    fun undo(): Boolean {
        val part = parts.lastOrNull() ?: return false
        val points = pointsOf(part)
        if (points.isEmpty()) parts.removeLast() else points.removeLast()
        return true
    }

    /** Returns problems that keep this session from being saved; empty when it is complete. */
    fun problems(): List<String> = buildList {
        if (parts.none { !it.excluded }) add("Die Region hat noch keinen eingeschlossenen Teil.")
        parts.forEachIndexed { i, part ->
            when (part) {
                is PrismPart -> if (part.points.size < 3) add("Teil ${i + 1} braucht mindestens drei Punkte.")
                is CuboidPart -> if (part.corners.size < 2) add("Teil ${i + 1} braucht zwei Ecken.")
            }
        }
    }

    /** Returns the mutable point list of [part]: the outline of a prism or the corners of a cuboid. */
    private fun pointsOf(part: EditorPart): MutableList<*> = when (part) {
        is PrismPart -> part.points
        is CuboidPart -> part.corners
    }
}
