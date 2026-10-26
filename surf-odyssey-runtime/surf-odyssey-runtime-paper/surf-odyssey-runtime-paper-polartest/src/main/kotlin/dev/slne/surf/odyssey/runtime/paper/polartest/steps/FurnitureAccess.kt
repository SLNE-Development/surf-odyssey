package dev.slne.surf.odyssey.runtime.paper.polartest.steps

import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture
import net.momirealms.craftengine.core.util.Key
import org.bukkit.Chunk
import org.bukkit.Location

/** Places and finds the CraftEngine test furniture. */
object FurnitureAccess {
    /** The id of the test furniture of the polartest CraftEngine pack. */
    const val FURNITURE_ID = "polartest:test_crate"

    /**
     * A furniture base entity as CraftEngine sees it.
     *
     * @property id the furniture id when CraftEngine tracks the entity as live furniture, otherwise `null`
     * @property blockX the block x of the base entity
     * @property blockY the block y of the base entity
     * @property blockZ the block z of the base entity
     */
    data class FoundFurniture(val id: String?, val blockX: Int, val blockY: Int, val blockZ: Int)

    /** Places the test furniture at [location] and returns whether CraftEngine placed it. Main thread only. */
    fun place(location: Location): Boolean = CraftEngineFurniture.place(location, Key.of(FURNITURE_ID)) != null

    /** Every furniture base entity in [chunk]. Main thread only. */
    fun furnitureIn(chunk: Chunk): List<FoundFurniture> = chunk.entities
        .filter { CraftEngineFurniture.isFurniture(it) }
        .map {
            val id = CraftEngineFurniture.getLoadedFurnitureByMetaEntity(it)?.id()?.asString()
            FoundFurniture(id, it.location.blockX, it.location.blockY, it.location.blockZ)
        }
}
