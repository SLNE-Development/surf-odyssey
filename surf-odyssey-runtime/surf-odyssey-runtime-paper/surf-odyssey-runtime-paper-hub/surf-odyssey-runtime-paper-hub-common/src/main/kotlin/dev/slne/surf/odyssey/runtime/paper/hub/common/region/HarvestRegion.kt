package dev.slne.surf.odyssey.runtime.paper.hub.common.region

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import kotlin.time.Duration

/**
 * A region in which players break the [breakable] blocks. A broken block turns into [placeholder]
 * and returns to its original state after [regenerateAfter]. [drops], when set, replaces the
 * vanilla drops of a broken block.
 *
 * @property breakable blocks players may break in this region
 * @property placeholder block a broken block turns into until it regenerates
 * @property regenerateAfter time until a broken block returns to its original state
 * @property drops returns the items a broken block drops instead of its vanilla drops, or `null`
 *           to keep the vanilla drops
 */
open class HarvestRegion(
    id: String,
    displayName: Component,
    shape: Shape,
    val breakable: Set<Material>,
    val placeholder: Material,
    val regenerateAfter: Duration,
    val drops: ((Block) -> Collection<ItemStack>)?,
) : Region(id, displayName, shape) {
    /** Returns whether [material] is one of the [breakable] blocks. */
    override fun canBreak(material: Material): Boolean = material in breakable
}

/** A mine: ores are broken, stone fills the gap until they regenerate. */
class MineRegion(
    id: String, displayName: Component, shape: Shape, breakable: Set<Material>,
    placeholder: Material, regenerateAfter: Duration, drops: ((Block) -> Collection<ItemStack>)?,
) : HarvestRegion(id, displayName, shape, breakable, placeholder, regenerateAfter, drops)

/** A forest: logs and leaves are broken and grow back after the delay. */
class ForestRegion(
    id: String, displayName: Component, shape: Shape, breakable: Set<Material>,
    regenerateAfter: Duration, drops: ((Block) -> Collection<ItemStack>)?,
) : HarvestRegion(id, displayName, shape, breakable, Material.AIR, regenerateAfter, drops)
