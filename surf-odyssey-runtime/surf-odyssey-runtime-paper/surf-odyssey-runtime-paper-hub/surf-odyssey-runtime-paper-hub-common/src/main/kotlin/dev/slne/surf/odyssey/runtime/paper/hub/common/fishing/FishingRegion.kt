package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import dev.slne.surf.odyssey.runtime.paper.hub.common.gate.GateDamage
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.ProtectionSettings
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import net.kyori.adventure.text.Component

/**
 * A fishing spot: catches whose hook lands inside come from [table] instead of vanilla loot.
 *
 * @param id unique id of the region
 * @param displayName name of the region shown to players
 * @param shape blocks the region covers; the hook position decides whether a catch counts
 * @property table the fish that can be caught here
 * @param settings changes allowed inside the region in addition to those the world allows
 * @property gate the requirement to act in this region, or `null` if it is open to everyone
 * @property gateDamage how this region hurts players its [gate] locks out
 */
class FishingRegion(
    id: String,
    displayName: Component,
    shape: Shape,
    val table: FishTable,
    settings: ProtectionSettings = ProtectionSettings(),
    override val gate: Requirement? = null,
    override val gateDamage: GateDamage = GateDamage(),
) : Region(id, displayName, shape, settings)
