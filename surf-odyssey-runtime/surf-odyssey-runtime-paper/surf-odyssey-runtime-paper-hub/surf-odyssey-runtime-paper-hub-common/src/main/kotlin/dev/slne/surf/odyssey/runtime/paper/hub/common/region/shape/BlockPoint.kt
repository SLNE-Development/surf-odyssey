package dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape

/** A block position in a world. */
data class BlockPoint(val x: Int, val y: Int, val z: Int)

/** A corner of a prism outline on the X/Z plane. */
data class Point2(val x: Int, val z: Int)
