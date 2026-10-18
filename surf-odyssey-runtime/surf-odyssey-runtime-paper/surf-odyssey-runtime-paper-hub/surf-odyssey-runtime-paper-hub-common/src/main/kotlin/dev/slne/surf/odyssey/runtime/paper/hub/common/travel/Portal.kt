package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement

/**
 * A portal area of a hub world.
 *
 * Stepping onto portal blocks inside [shape] sends players to [targetCategory]. Players who come
 * from [targetCategory] arrive at [arrival] in front of this portal.
 *
 * @property id unique name of the portal within its world
 * @property shape the blocks the portal covers
 * @property targetCategory the server category the portal leads to
 * @property arrival where players coming from [targetCategory] appear
 * @property gate the requirement a player must meet to travel through the portal, or `null` if it is open to everyone
 */
data class Portal(
    val id: String,
    val shape: Shape,
    val targetCategory: String,
    val arrival: WorldPosition,
    val gate: Requirement? = null,
)
