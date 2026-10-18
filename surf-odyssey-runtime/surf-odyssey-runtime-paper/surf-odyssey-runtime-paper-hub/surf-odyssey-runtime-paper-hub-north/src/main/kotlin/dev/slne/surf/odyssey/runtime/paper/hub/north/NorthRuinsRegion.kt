package dev.slne.surf.odyssey.runtime.paper.hub.north

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.shape
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** The ruins of North, a region only this world has; it greets players who enter. */
class NorthRuinsRegion : Region(
    "north_ruins",
    Component.text("Alte Ruinen"),
    shape { cuboid(500, 60, 500, 540, 90, 540) },
) {
    /** Greets [player] on entering. */
    override fun onEnter(player: Player) {
        player.sendText { info("Ein kalter Wind weht durch die alten Ruinen.") }
    }
}
