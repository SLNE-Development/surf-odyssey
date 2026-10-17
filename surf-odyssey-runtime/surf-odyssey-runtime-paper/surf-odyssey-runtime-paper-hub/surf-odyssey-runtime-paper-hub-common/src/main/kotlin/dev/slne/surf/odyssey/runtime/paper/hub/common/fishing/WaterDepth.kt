package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

/** Counts the water under a fishing hook. */
object WaterDepth {
    /**
     * Returns the number of water blocks in the column under a hook, at most [limit].
     *
     * [isWaterAt] tells whether the block at a vertical offset from the hook block is water: 0 is the
     * hook block, -1 the block below it. Counting starts at the hook block, or at the block below it
     * when the hook block is not water, and stops at the first block that is not water.
     */
    fun count(isWaterAt: (Int) -> Boolean, limit: Int): Int {
        var offset = if (isWaterAt(0)) 0 else -1
        var depth = 0
        while (depth < limit && isWaterAt(offset)) {
            depth++
            offset--
        }
        return depth
    }
}
