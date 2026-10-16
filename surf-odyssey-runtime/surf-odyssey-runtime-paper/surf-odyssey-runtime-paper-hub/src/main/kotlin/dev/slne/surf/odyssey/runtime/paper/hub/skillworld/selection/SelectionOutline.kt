package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.selection

import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockBox
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockPos

data class OutlinePoint(val x: Double, val y: Double, val z: Double)

fun selectionOutline(a: BlockPos, b: BlockPos, maxPoints: Int): List<OutlinePoint> {
    val box = BlockBox.of(a, b)
    val lengthX = box.maxX - box.minX + 1
    val lengthY = box.maxY - box.minY + 1
    val lengthZ = box.maxZ - box.minZ + 1

    val longest = maxOf(lengthX, lengthY, lengthZ)
    val step = (1..longest).firstOrNull { pointCount(lengthX, lengthY, lengthZ, it) <= maxPoints } ?: longest

    val xs = offsets(lengthX, step).map { box.minX + it.toDouble() }
    val ys = offsets(lengthY, step).map { box.minY + it.toDouble() }
    val zs = offsets(lengthZ, step).map { box.minZ + it.toDouble() }
    val loX = xs.first()
    val hiX = xs.last()
    val loY = ys.first()
    val hiY = ys.last()
    val loZ = zs.first()
    val hiZ = zs.last()

    val points = LinkedHashSet<OutlinePoint>()
    for (y in listOf(loY, hiY)) for (z in listOf(loZ, hiZ)) xs.forEach { points += OutlinePoint(it, y, z) }
    for (x in listOf(loX, hiX)) for (z in listOf(loZ, hiZ)) ys.forEach { points += OutlinePoint(x, it, z) }
    for (x in listOf(loX, hiX)) for (y in listOf(loY, hiY)) zs.forEach { points += OutlinePoint(x, y, it) }

    return points.toList()
}

private fun offsets(length: Int, step: Int): List<Int> = (0 until length step step).toList() + length

private fun pointCount(lengthX: Int, lengthY: Int, lengthZ: Int, step: Int): Int {
    fun interior(length: Int) = offsets(length, step).size - 2
    return 8 + 4 * (interior(lengthX) + interior(lengthY) + interior(lengthZ))
}
