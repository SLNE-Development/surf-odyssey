package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape

/**
 * A portal area of a hub world; stepping onto portal blocks inside [shape] sends players to [targetCategory].
 *
 * @property id unique name of the portal within its world
 * @property shape the blocks the portal covers
 * @property targetCategory the server category the portal leads to
 */
data class Portal(val id: String, val shape: Shape, val targetCategory: String)
