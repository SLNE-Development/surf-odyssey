package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.ProtectionSettings
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.ProtectionSettingsBuilder
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.protectionSettings
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.RegionDsl
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.ShapeBuilder
import net.kyori.adventure.text.Component
import org.bukkit.entity.EntityType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Settings of a mob spawn region. */
@RegionDsl
class MobSpawnRegionBuilder(private val id: String) {
    init {
        require(Region.ID_PATTERN.matches(id)) { "Invalid region id '$id', use lowercase letters, digits and underscores" }
    }

    /** Name shown to players, defaults to the id. */
    var displayName: Component = Component.text(id)

    /** Time between two spawns. */
    var respawnInterval: Duration = 30.seconds

    /** Most mobs alive at once. */
    var maxAlive: Int = 5

    private var shape: Shape? = null
    private val types = mutableListOf<EntityType>()
    private val points = mutableListOf<BlockPoint>()
    private var settings = ProtectionSettings()

    /** Sets the changes allowed inside the region in addition to those the world allows. */
    fun settings(block: ProtectionSettingsBuilder.() -> Unit) {
        settings = protectionSettings(block)
    }

    /** Sets the shape of the region. */
    fun shape(block: ShapeBuilder.() -> Unit) {
        shape = ShapeBuilder().apply(block).build()
    }

    /** Adds mob types to pick from. */
    fun mobs(vararg types: EntityType) {
        this.types += types
    }

    /** Adds a block mobs can spawn on. */
    fun spawnPoint(x: Int, y: Int, z: Int) {
        points += BlockPoint(x, y, z)
    }

    /** Creates the region from the collected settings, failing when a spawn point lies outside the shape. */
    internal fun build() = MobSpawnRegion(
        id, displayName, requireNotNull(shape) { "Region '$id' has no shape" },
        types.toList(), points.toList(), respawnInterval, maxAlive, settings,
    )
}
