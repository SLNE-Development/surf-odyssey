package dev.slne.surf.odyssey.runtime.paper.hub.common.region

import dev.slne.surf.odyssey.runtime.paper.hub.common.gate.GateDamage
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.ProtectionSettings
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A region in which players break the [breakable] blocks. A broken block turns into [placeholder]
 * and returns to its original state after [regenerateAfter]. [drops], when set, replaces the
 * vanilla drops of a broken block.
 *
 * @property breakable blocks players may break in this region
 * @property placeholder block a broken block turns into until it regenerates
 * @property regenerateAfter time until a broken block returns to its original state, at least
 *           [MIN_REGENERATE_AFTER]
 * @property drops returns the items a broken block drops instead of its vanilla drops, or `null`
 *           to keep the vanilla drops
 * @property gate the requirement to act in this region, or `null` if it is open to everyone
 * @property gateDamage how this region hurts players its [gate] locks out
 */
open class HarvestRegion(
    id: String,
    displayName: Component,
    shape: Shape,
    val breakable: Set<Material>,
    val placeholder: Material,
    val regenerateAfter: Duration,
    val drops: ((Block) -> Collection<ItemStack>)?,
    settings: ProtectionSettings = ProtectionSettings(),
    override val gate: Requirement? = null,
    override val gateDamage: GateDamage = GateDamage(),
) : Region(id, displayName, shape, settings) {
    init {
        require(regenerateAfter >= MIN_REGENERATE_AFTER) {
            "Region '$id' regenerates after $regenerateAfter, at least $MIN_REGENERATE_AFTER is required"
        }
    }

    /** Returns whether [material] is one of the [breakable] blocks. */
    override fun canBreak(material: Material): Boolean = material in breakable

    companion object {
        /** Shortest allowed [regenerateAfter]; regeneration runs once per second. */
        val MIN_REGENERATE_AFTER = 1.seconds
    }
}

/** A mine: ores are broken, stone fills the gap until they regenerate. */
class MineRegion(
    id: String, displayName: Component, shape: Shape, breakable: Set<Material>,
    placeholder: Material, regenerateAfter: Duration, drops: ((Block) -> Collection<ItemStack>)?,
    settings: ProtectionSettings = ProtectionSettings(),
    gate: Requirement? = null, gateDamage: GateDamage = GateDamage(),
) : HarvestRegion(id, displayName, shape, breakable, placeholder, regenerateAfter, drops, settings, gate, gateDamage)

/** A forest: logs and leaves are broken and grow back after the delay. */
class ForestRegion(
    id: String, displayName: Component, shape: Shape, breakable: Set<Material>,
    regenerateAfter: Duration, drops: ((Block) -> Collection<ItemStack>)?,
    settings: ProtectionSettings = ProtectionSettings(),
    gate: Requirement? = null, gateDamage: GateDamage = GateDamage(),
) : HarvestRegion(id, displayName, shape, breakable, Material.AIR, regenerateAfter, drops, settings, gate, gateDamage)
