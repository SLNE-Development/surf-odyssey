package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.RegionDsl
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.ShapeBuilder
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

/** Settings of a fishing region. */
@RegionDsl
class FishingRegionBuilder(private val id: String) {
    init {
        require(Region.ID_PATTERN.matches(id)) { "Invalid region id '$id', use lowercase letters, digits and underscores" }
    }

    /** Name shown to players, defaults to the id. */
    var displayName: Component = Component.text(id)
    private var shape: Shape? = null
    private val fish = mutableListOf<FishEntry>()

    /** Sets the shape of the region; the hook position decides whether a catch counts. */
    fun shape(block: ShapeBuilder.() -> Unit) {
        shape = ShapeBuilder().apply(block).build()
    }

    /** Adds a fish with [weight] whose conditions and item are set in [block]. */
    fun fish(id: String, weight: Int, block: FishBuilder.() -> Unit) {
        fish += FishBuilder(id, weight).apply(block).build()
    }

    /** Creates the fishing region from the collected settings. */
    internal fun build() = FishingRegion(id, displayName, requireNotNull(shape) { "Region '$id' has no shape" }, FishTable(fish.toList()))
}

/** Conditions and item of one fish. */
@RegionDsl
class FishBuilder(private val id: String, private val weight: Int) {
    private val conditions = mutableListOf<FishCondition>()
    private var item: (() -> ItemStack)? = null

    /** Adds a condition. */
    fun condition(condition: FishCondition) {
        conditions += condition
    }

    /** Requires rain. */
    fun rain() = condition(FishCondition.rain())

    /** Requires a thunderstorm. */
    fun thunder() = condition(FishCondition.thunder())

    /** Requires night. */
    fun night() = condition(FishCondition.night())

    /** Requires day. */
    fun day() = condition(FishCondition.day())

    /** Requires one of the biomes with the given keys. */
    fun biome(vararg keys: String) = condition(FishCondition.biome(*keys))

    /** Requires water at least [blocks] deep. */
    fun minDepth(blocks: Int) = condition(FishCondition.minDepth(blocks))

    /** Sets the item a catch of this fish gives. */
    fun item(provider: () -> ItemStack) {
        item = provider
    }

    /** Creates the fish from the collected settings. */
    internal fun build() = FishEntry(id, weight, conditions.toList(), requireNotNull(item) { "Fish '$id' has no item" })
}
