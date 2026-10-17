package dev.slne.surf.odyssey.runtime.paper.hub.common.region

import dev.slne.surf.odyssey.runtime.paper.hub.common.fishing.FishingRegionBuilder
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.RegionDsl
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.ShapeBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Collects the regions a hub world declares. */
@RegionDsl
class RegionRegistry {
    private val regions = mutableListOf<Region>()

    /** Adds a region implemented by its own class. */
    fun region(region: Region) {
        regions += region
    }

    /** Adds a mine configured in [block]. */
    fun mine(id: String, block: HarvestRegionBuilder.() -> Unit) {
        regions += HarvestRegionBuilder(id, Material.STONE).apply(block).buildMine()
    }

    /** Adds a forest configured in [block]. */
    fun forest(id: String, block: HarvestRegionBuilder.() -> Unit) {
        regions += HarvestRegionBuilder(id, Material.AIR).apply(block).buildForest()
    }

    /** Adds a fishing region configured in [block]. */
    fun fishing(id: String, block: FishingRegionBuilder.() -> Unit) {
        regions += FishingRegionBuilder(id).apply(block).build()
    }

    /** Returns all regions in declaration order. */
    fun build(): List<Region> = regions.toList()
}

/** Settings of a mine or forest. */
@RegionDsl
class HarvestRegionBuilder(private val id: String, defaultPlaceholder: Material) {
    init {
        require(Region.ID_PATTERN.matches(id)) { "Invalid region id '$id', use lowercase letters, digits and underscores" }
    }

    /** Name shown to players, defaults to the id. */
    var displayName: Component = Component.text(id)

    /** Block a broken block turns into until it regenerates. Ignored for forests. */
    var placeholder: Material = defaultPlaceholder

    /** Time until a broken block returns. */
    var regenerateAfter: Duration = 5.minutes

    private var shape: Shape? = null
    private val breakable = mutableSetOf<Material>()
    private var drops: ((Block) -> Collection<ItemStack>)? = null

    /** Sets the shape of the region. */
    fun shape(block: ShapeBuilder.() -> Unit) {
        shape = ShapeBuilder().apply(block).build()
    }

    /** Allows breaking blocks of the given [materials]. */
    fun breakable(vararg materials: Material) {
        breakable += materials
    }

    /** Replaces the vanilla drops of broken blocks with those [provider] returns. */
    fun drops(provider: (Block) -> Collection<ItemStack>) {
        drops = provider
    }

    /** Creates the mine from the collected settings. */
    internal fun buildMine() = MineRegion(id, displayName, requireShape(), breakable.toSet(), placeholder, regenerateAfter, drops)

    /** Creates the forest from the collected settings. */
    internal fun buildForest() = ForestRegion(id, displayName, requireShape(), breakable.toSet(), regenerateAfter, drops)

    /** Returns the configured shape or fails when none was set. */
    private fun requireShape(): Shape = requireNotNull(shape) { "Region '$id' has no shape" }
}
