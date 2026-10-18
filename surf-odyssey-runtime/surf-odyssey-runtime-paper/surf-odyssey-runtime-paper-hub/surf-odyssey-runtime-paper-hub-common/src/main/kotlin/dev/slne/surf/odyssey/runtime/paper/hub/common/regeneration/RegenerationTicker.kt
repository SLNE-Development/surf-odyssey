package dev.slne.surf.odyssey.runtime.paper.hub.common.regeneration

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import org.bukkit.World
import org.bukkit.block.data.BlockData

/** Restores harvested blocks of the hub world once their delay has passed. */
object RegenerationTicker {
    /** Blocks waiting to regenerate, with their original block data. */
    val queue = RegenerationQueue<BlockData>()

    /** Schedules the block at [point] to return to [original] after [delayMillis]. */
    fun schedule(point: BlockPoint, original: BlockData, delayMillis: Long): Boolean =
        queue.schedule(point, original, System.currentTimeMillis() + delayMillis)

    /** Restores every block that is due; called once per second on the main thread. */
    fun tick(world: World) = restore(world, System.currentTimeMillis())

    /** Restores every waiting block at once, whether due or not; called on the main thread. */
    fun restoreAll(world: World) = restore(world, Long.MAX_VALUE)

    /** Restores every block in [world] that is due at [now]. */
    private fun restore(world: World, now: Long) {
        for (entry in queue.pollDue(now)) {
            world.getBlockAt(entry.point.x, entry.point.y, entry.point.z).setBlockData(entry.original, false)
        }
    }
}
