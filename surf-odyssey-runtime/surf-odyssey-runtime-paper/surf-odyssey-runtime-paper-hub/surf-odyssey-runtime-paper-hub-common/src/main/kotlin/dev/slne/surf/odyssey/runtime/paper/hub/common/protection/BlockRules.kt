package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.HarvestRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import org.bukkit.Material

/** What happens to a block a player tries to break. */
enum class BreakDecision {
    /** The break is cancelled. */
    DENY,

    /** The block breaks normally and does not regenerate. */
    ALLOW,

    /** The block breaks, turns into the region's placeholder and regenerates later. */
    HARVEST,
}

/** Decides whether and how a block may be broken in a hub world. */
object BlockRules {
    /**
     * Returns the decision for breaking a block of [material] in [region] (`null` outside every
     * region). [pending] tells whether the block is waiting to regenerate, [building] whether the
     * player is in build mode.
     */
    fun decide(region: Region?, material: Material, pending: Boolean, building: Boolean): BreakDecision = when {
        building -> BreakDecision.ALLOW
        region == null || pending || !region.canBreak(material) -> BreakDecision.DENY
        region is HarvestRegion -> BreakDecision.HARVEST
        else -> BreakDecision.ALLOW
    }
}
