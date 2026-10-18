package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import kotlin.math.ceil
import kotlin.math.sqrt

/** Computes how the editor preview draws an edge: how many particles and how far it is from the viewer. */
object Preview {
    /** Distance between two particles along an edge, in blocks. */
    const val STEP = 0.5

    /** Most particles drawn along a single edge. */
    const val MAX_STEPS = 256

    /** Returns the number of steps for an edge of [length] blocks: one per [STEP], at least 1, at most [MAX_STEPS]. */
    fun steps(length: Double): Int = ceil(length / STEP).toInt().coerceIn(1, MAX_STEPS)

    /** Returns the length of the edge from [a] to [b]. */
    fun length(a: Vec, b: Vec): Double = norm(b.first - a.first, b.second - a.second, b.third - a.third)

    /** Returns the distance from [point] to the nearest point of the edge from [a] to [b]. */
    fun distance(point: Vec, a: Vec, b: Vec): Double {
        val dx = b.first - a.first
        val dy = b.second - a.second
        val dz = b.third - a.third
        val lengthSquared = dx * dx + dy * dy + dz * dz
        val projected = (point.first - a.first) * dx + (point.second - a.second) * dy + (point.third - a.third) * dz
        val t = if (lengthSquared == 0.0) 0.0 else (projected / lengthSquared).coerceIn(0.0, 1.0)
        return norm(a.first + dx * t - point.first, a.second + dy * t - point.second, a.third + dz * t - point.third)
    }

    /** Returns the length of the vector [x], [y], [z]. */
    private fun norm(x: Double, y: Double, z: Double): Double = sqrt(x * x + y * y + z * z)
}
