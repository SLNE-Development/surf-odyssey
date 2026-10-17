package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal

import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockBox
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockPos

object PortalBlocks {
    const val SCAN_LIMIT = 1_000_000L

    fun bodyCells(x: Int, y: Int, z: Int): List<BlockPos> = listOf(BlockPos(x, y, z), BlockPos(x, y + 1, z))

    data class ScanResult(val blocks: Int, val skippedChunks: Int)

    fun countPortalBlocks(
        box: BlockBox,
        isChunkLoaded: (Int, Int) -> Boolean,
        isPortalBlock: (Int, Int, Int) -> Boolean,
    ): ScanResult {
        var count = 0
        var skipped = 0
        for (chunkX in (box.minX shr 4)..(box.maxX shr 4)) {
            for (chunkZ in (box.minZ shr 4)..(box.maxZ shr 4)) {
                if (!isChunkLoaded(chunkX, chunkZ)) {
                    skipped++
                    continue
                }
                for (x in maxOf(box.minX, chunkX shl 4)..minOf(box.maxX, (chunkX shl 4) + 15)) {
                    for (z in maxOf(box.minZ, chunkZ shl 4)..minOf(box.maxZ, (chunkZ shl 4) + 15)) {
                        for (y in box.minY..box.maxY) {
                            if (isPortalBlock(x, y, z)) count++
                        }
                    }
                }
            }
        }
        return ScanResult(count, skipped)
    }

    fun exceedsScanLimit(box: BlockBox, limit: Long = SCAN_LIMIT): Boolean = box.volume > limit

    fun triggers(portals: List<Portal>, cells: List<BlockPos>, isPortalBlock: (Int, Int, Int) -> Boolean): Portal? =
        portals.firstOrNull { portal ->
            cells.any { cell -> portal.box.contains(cell.x, cell.y, cell.z) && isPortalBlock(cell.x, cell.y, cell.z) }
        }
}
