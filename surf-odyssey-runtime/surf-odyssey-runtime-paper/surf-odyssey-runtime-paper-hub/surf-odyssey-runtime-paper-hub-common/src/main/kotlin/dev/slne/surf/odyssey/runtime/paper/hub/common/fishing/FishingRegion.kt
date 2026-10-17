package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import net.kyori.adventure.text.Component

/**
 * A fishing spot: catches whose hook lands inside come from [table] instead of vanilla loot.
 *
 * @property table the fish that can be caught here
 */
class FishingRegion(id: String, displayName: Component, shape: Shape, val table: FishTable) : Region(id, displayName, shape)
