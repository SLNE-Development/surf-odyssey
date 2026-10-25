package dev.slne.surf.odyssey.runtime.paper.polartest.layout

import kotlinx.serialization.Serializable

/** A block position in world coordinates. */
@Serializable
data class BlockPos(val x: Int, val y: Int, val z: Int)
