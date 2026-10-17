package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

/** A point in world space used to draw outlines. */
typealias Vec = Triple<Double, Double, Double>

/** Computes the edges drawn as particle lines around an editor part. */
object ShapeOutline {
    /** Returns the edges of [part] along block borders; incomplete parts have none. */
    fun edges(part: EditorPart): List<Pair<Vec, Vec>> = when (part) {
        is CuboidPart -> if (part.corners.size < 2) emptyList() else cuboidEdges(part)
        is PrismPart -> if (part.points.size < 3) emptyList() else prismEdges(part)
    }

    /** Returns the twelve edges of the box around both corners of [part], on the outer block faces. */
    private fun cuboidEdges(part: CuboidPart): List<Pair<Vec, Vec>> {
        val (a, b) = part.corners
        val x0 = minOf(a.x, b.x).toDouble(); val x1 = maxOf(a.x, b.x) + 1.0
        val y0 = minOf(a.y, b.y).toDouble(); val y1 = maxOf(a.y, b.y) + 1.0
        val z0 = minOf(a.z, b.z).toDouble(); val z1 = maxOf(a.z, b.z) + 1.0
        val c = listOf(Vec(x0, y0, z0), Vec(x1, y0, z0), Vec(x1, y0, z1), Vec(x0, y0, z1), Vec(x0, y1, z0), Vec(x1, y1, z0), Vec(x1, y1, z1), Vec(x0, y1, z1))
        return listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7).map { c[it.first] to c[it.second] }
    }

    /** Returns the bottom ring, top ring and vertical corner edges of [part], through the block centres of its corners. */
    private fun prismEdges(part: PrismPart): List<Pair<Vec, Vec>> {
        val bottom = part.minY.toDouble()
        val top = part.maxY + 1.0
        val corners = part.points.map { it.x + 0.5 to it.z + 0.5 }
        val ring = corners.indices.flatMap { i ->
            val (ax, az) = corners[i]
            val (bx, bz) = corners[(i + 1) % corners.size]
            listOf(Vec(ax, bottom, az) to Vec(bx, bottom, bz), Vec(ax, top, az) to Vec(bx, top, bz))
        }
        return ring + corners.map { (x, z) -> Vec(x, bottom, z) to Vec(x, top, z) }
    }
}
