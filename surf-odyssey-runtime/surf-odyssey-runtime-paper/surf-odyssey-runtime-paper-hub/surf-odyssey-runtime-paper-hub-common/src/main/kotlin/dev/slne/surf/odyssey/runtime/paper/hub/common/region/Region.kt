package dev.slne.surf.odyssey.runtime.paper.hub.common.region

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent

/**
 * An area of a hub world with its own rules.
 *
 * Outside every region nothing can be broken. Subclasses decide what may be broken inside and
 * react to players through the hooks, which are called on the main thread.
 *
 * @property id unique id of the region, matching [ID_PATTERN]
 * @property displayName name of the region shown to players
 * @property shape blocks the region covers
 */
abstract class Region(val id: String, val displayName: Component, val shape: Shape) {
    init {
        require(ID_PATTERN.matches(id)) { "Invalid region id '$id', use lowercase letters, digits and underscores" }
    }

    /** Returns whether a player may break a block of [material] in this region. */
    open fun canBreak(material: Material): Boolean = false

    /** Called when [player] walks into this region. */
    open fun onEnter(player: Player) {}

    /** Called when [player] walks out of this region or leaves the server inside it. */
    open fun onLeave(player: Player) {}

    /** Called after a break in this region was allowed; cancelling [event] still prevents it. */
    open fun onBreak(event: BlockBreakEvent) {}

    /** Called when a player interacts with a block inside this region. */
    open fun onInteract(event: PlayerInteractEvent) {}

    /** Called once per second. */
    open fun onTick() {}

    companion object {
        /** Allowed format of region ids. */
        val ID_PATTERN = Regex("[a-z0-9_]+")
    }
}
