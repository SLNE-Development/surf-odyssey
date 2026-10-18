package dev.slne.surf.odyssey.runtime.paper.hub.common.region

import dev.slne.surf.odyssey.runtime.paper.hub.common.mobs.MobSpawnRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Bounds

/**
 * Finds the region that covers a block.
 *
 * Construction fails when two regions share an id or at least one block, so every block belongs
 * to at most one region.
 */
class RegionIndex(regions: List<Region>) {
    /** All regions in declaration order. */
    val regions: List<Region> = regions.toList()
    private val byId = this.regions.associateBy { it.id }

    /** The mob spawn regions among [regions], in declaration order. */
    val mobSpawnRegions: List<MobSpawnRegion> = this.regions.filterIsInstance<MobSpawnRegion>()

    init {
        val duplicates = this.regions.groupBy { it.id }.filterValues { it.size > 1 }.keys
        check(duplicates.isEmpty()) { "Duplicate region ids: ${duplicates.joinToString()}" }
        val overlaps = overlaps(this.regions)
        check(overlaps.isEmpty()) { "Overlapping regions: ${overlaps.joinToString { "${it.first} and ${it.second}" }}" }
    }

    /** Returns the region covering the block at [x], [y], [z], or `null` outside every region. */
    fun regionAt(x: Int, y: Int, z: Int): Region? = regions.firstOrNull { it.shape.contains(x, y, z) }

    /** Returns the region with [id], or `null`. */
    fun byId(id: String): Region? = byId[id]

    companion object {
        /** Returns the id pairs of regions that share at least one block, scanning only where their bounds meet. */
        fun overlaps(regions: List<Region>): List<Pair<String, String>> {
            val result = mutableListOf<Pair<String, String>>()
            for (i in regions.indices) for (j in i + 1 until regions.size) {
                val a = regions[i]
                val b = regions[j]
                val shared = a.shape.bounds.intersection(b.shape.bounds) ?: continue
                if (sharesBlock(a, b, shared)) result += a.id to b.id
            }
            return result
        }

        /** Returns whether any block inside [area] belongs to both [a] and [b]. */
        private fun sharesBlock(a: Region, b: Region, area: Bounds): Boolean {
            for (x in area.minX..area.maxX) for (y in area.minY..area.maxY) for (z in area.minZ..area.maxZ) {
                if (a.shape.contains(x, y, z) && b.shape.contains(x, y, z)) return true
            }
            return false
        }
    }
}
