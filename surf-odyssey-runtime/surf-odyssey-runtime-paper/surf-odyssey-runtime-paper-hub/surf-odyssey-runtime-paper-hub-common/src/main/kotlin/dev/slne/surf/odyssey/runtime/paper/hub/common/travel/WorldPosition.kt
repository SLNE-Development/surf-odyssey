package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import org.bukkit.Location
import org.bukkit.World

/**
 * A position with view direction in the hub world.
 *
 * @property x the x coordinate
 * @property y the y coordinate
 * @property z the z coordinate
 * @property yaw the horizontal view angle in degrees
 * @property pitch the vertical view angle in degrees
 */
data class WorldPosition(val x: Double, val y: Double, val z: Double, val yaw: Float = 0f, val pitch: Float = 0f) {
    /** Returns this position as a location in [world]. */
    fun toLocation(world: World) = Location(world, x, y, z, yaw, pitch)
}
