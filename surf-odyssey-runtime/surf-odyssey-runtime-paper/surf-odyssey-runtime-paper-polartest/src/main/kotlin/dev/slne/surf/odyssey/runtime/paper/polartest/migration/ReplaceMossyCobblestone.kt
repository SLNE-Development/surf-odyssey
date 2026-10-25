package dev.slne.surf.odyssey.runtime.paper.polartest.migration

import dev.slne.surf.odyssey.runtime.paper.polartest.layout.TestLayout
import dev.slne.surf.odyssey.runtime.paper.polartest.session.LIVE_MIGRATED_MARKER
import dev.slne.surf.polarpaper.api.migration.WorldMigration
import dev.slne.surf.polarpaper.api.world.SurfChunk
import org.bukkit.Material
import org.bukkit.persistence.PersistentDataType

/**
 * Live stage of world version 3: turns mossy cobblestone between [TestLayout.FLOOR_Y] and [TestLayout.TOP_Y] into
 * stone and marks every chunk it visited with [LIVE_MIGRATED_MARKER] in the chunk's persistent data.
 */
object ReplaceMossyCobblestone : WorldMigration(3) {
    /** Always `true`: this migration only has a live stage. */
    override val hasLiveStage get() = true

    /** Replaces the mossy cobblestone of [chunk] and sets its marker. */
    override fun migrateLive(chunk: SurfChunk) {
        val bukkit = chunk.bukkitChunk
        for (x in 0..15) for (z in 0..15) for (y in TestLayout.FLOOR_Y..TestLayout.TOP_Y) {
            val block = bukkit.getBlock(x, y, z)
            if (block.type == Material.MOSSY_COBBLESTONE) block.setType(Material.STONE, false)
        }
        bukkit.persistentDataContainer.set(LIVE_MIGRATED_MARKER, PersistentDataType.BOOLEAN, true)
    }
}
