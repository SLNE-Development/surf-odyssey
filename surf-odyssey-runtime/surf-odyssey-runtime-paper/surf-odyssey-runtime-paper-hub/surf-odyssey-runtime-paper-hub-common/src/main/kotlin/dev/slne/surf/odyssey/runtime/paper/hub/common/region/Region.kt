package dev.slne.surf.odyssey.runtime.paper.hub.common.region

import dev.slne.surf.odyssey.runtime.paper.hub.common.gate.GateDamage
import dev.slne.surf.odyssey.runtime.paper.hub.common.protection.ProtectionSettings
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.Shape
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
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
 * @property settings changes allowed inside the region in addition to those the world allows
 */
abstract class Region(
    val id: String,
    val displayName: Component,
    val shape: Shape,
    val settings: ProtectionSettings = ProtectionSettings(),
) {
    init {
        require(ID_PATTERN.matches(id)) { "Invalid region id '$id', use lowercase letters, digits and underscores" }
    }

    /**
     * The requirement a player must meet to act in this region, or `null` if the region is open to everyone. Players
     * who do not meet it are darkened and hurt inside, and their actions here are cancelled.
     */
    open val gate: Requirement? = null

    /** How this region hurts players its [gate] locks out. */
    open val gateDamage: GateDamage = GateDamage()

    /** Returns whether a player may break a block of [material] in this region. */
    open fun canBreak(material: Material): Boolean = false

    /** Called when [player] walks into this region. */
    open fun onEnter(player: Player) {}

    /** Called when [player] walks out of this region or leaves the server inside it. */
    open fun onLeave(player: Player) {}

    /**
     * Called when a player is allowed to break a block in this region, before
     * the block breaks or is scheduled to regenerate; cancelling [event] still prevents both.
     */
    open fun onBreak(event: BlockBreakEvent) {}

    /**
     * Called when a player interacts with a block inside this region, including uses the protection
     * settings deny. [PlayerInteractEvent.useInteractedBlock] tells whether the block use is still
     * going to happen; setting it to `ALLOW` permits a denied use and `DENY` forbids it. The hook must
     * not assume the use succeeds, since later event handlers may still change the result.
     */
    open fun onInteract(event: PlayerInteractEvent) {}

    /** Called once per second. */
    open fun onTick() {}

    companion object {
        /** Allowed format of region ids. */
        val ID_PATTERN = Regex("[a-z0-9_]+")
    }
}
