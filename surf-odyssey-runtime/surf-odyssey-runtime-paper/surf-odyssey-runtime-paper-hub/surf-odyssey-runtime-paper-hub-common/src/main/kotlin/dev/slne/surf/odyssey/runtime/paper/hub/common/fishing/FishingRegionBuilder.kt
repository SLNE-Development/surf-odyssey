package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.ProtectionSettings
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.ProtectionSettingsBuilder
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.protectionSettings
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.RegionDsl
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.ShapeBuilder
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

/** Collects the settings of the fishing region [id]: its name, shape, fish and protection settings. */
@RegionDsl
class FishingRegionBuilder(private val id: String) {
    init {
        require(Region.ID_PATTERN.matches(id)) { "Invalid region id '$id', use lowercase letters, digits and underscores" }
    }

    /** Name shown to players, defaults to the id. */
    var displayName: Component = Component.text(id)
    private var shape: Shape? = null
    private val fish = mutableListOf<FishEntry>()
    private var settings = ProtectionSettings()

    /** Sets the changes allowed inside the region in addition to those the world allows. */
    fun settings(block: ProtectionSettingsBuilder.() -> Unit) {
        settings = protectionSettings(block)
    }

    /** Sets the shape of the region; the hook position decides whether a catch counts. */
    fun shape(block: ShapeBuilder.() -> Unit) {
        shape = ShapeBuilder().apply(block).build()
    }

    /** Adds a fish with [weight] whose conditions and item are set in [block]. */
    fun fish(id: String, weight: Int, block: FishBuilder.() -> Unit) {
        fish += FishBuilder(id, weight).apply(block).build()
    }

    /** Creates the fishing region from the collected settings, failing when it has no shape or no fish. */
    internal fun build(): FishingRegion {
        require(fish.isNotEmpty()) { "Fishing region '$id' needs at least one fish" }
        return FishingRegion(id, displayName, requireNotNull(shape) { "Region '$id' has no shape" }, FishTable(fish.toList()), settings)
    }
}

/** Collects the conditions and the item of the fish [id] with its relative [weight]. */
@RegionDsl
class FishBuilder(private val id: String, private val weight: Int) {
    private val conditions = mutableListOf<FishCondition>()
    private var item: (() -> ItemStack)? = null

    /** Adds a condition. */
    fun condition(condition: FishCondition) {
        conditions += condition
    }

    /** Requires a storm in the world. */
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
