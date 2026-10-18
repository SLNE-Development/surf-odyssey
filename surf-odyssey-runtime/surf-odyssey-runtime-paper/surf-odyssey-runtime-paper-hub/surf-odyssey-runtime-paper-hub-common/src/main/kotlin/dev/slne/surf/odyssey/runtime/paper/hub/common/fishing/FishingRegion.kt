package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.ProtectionSettings
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import net.kyori.adventure.text.Component

/**
 * A fishing spot: catches whose hook lands inside come from [table] instead of vanilla loot.
 *
 * @param id unique id of the region
 * @param displayName name of the region shown to players
 * @param shape blocks the region covers; the hook position decides whether a catch counts
 * @property table the fish that can be caught here
 * @param settings changes allowed inside the region in addition to those the world allows
 */
class FishingRegion(
    id: String,
    displayName: Component,
    shape: Shape,
    val table: FishTable,
    settings: ProtectionSettings = ProtectionSettings(),
) : Region(id, displayName, shape, settings)
