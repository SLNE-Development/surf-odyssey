package dev.slne.surf.odyssey.runtime.paper.polartest.migration

import dev.slne.surf.polarpaper.api.migration.RawWorld
import dev.slne.surf.polarpaper.api.migration.WorldMigration

/** Raw stage of world version 2: turns every plain cobblestone block into air and keeps all other blocks. */
object RemoveCobblestone : WorldMigration(2) {
    /** Scans every block of every stored chunk from the lowest to the highest section. */
    override fun migrateRaw(world: RawWorld) {
        val minY = world.minSection * 16
        val maxY = (world.maxSection + 1) * 16 - 1

        for (chunk in world.chunks) {
            for (x in 0..15) for (z in 0..15) for (y in minY..maxY) {
                if (isCobblestone(chunk.getBlock(x, y, z))) chunk.setBlock(x, y, z, "minecraft:air")
            }
        }
    }

    /** Whether [state] is plain cobblestone; mossy cobblestone, slabs, stairs and walls are not. */
    fun isCobblestone(state: String): Boolean =
        state == "minecraft:cobblestone" || state.startsWith("minecraft:cobblestone[")
}
