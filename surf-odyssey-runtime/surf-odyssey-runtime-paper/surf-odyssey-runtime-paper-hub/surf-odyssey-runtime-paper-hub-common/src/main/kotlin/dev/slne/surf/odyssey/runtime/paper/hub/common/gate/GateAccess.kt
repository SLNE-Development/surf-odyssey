package dev.slne.surf.odyssey.runtime.paper.hub.common.gate

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPermissions
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import dev.slne.surf.odyssey.runtime.paper.progression.state.PlayerRequirementContext
import org.bukkit.Location
import org.bukkit.entity.Player

/** Checks gates against online players. Every function must run on the thread of the player it is given. */
object GateAccess {
    /** Shown in the action bar inside a region the player is locked out of. */
    const val REGION_LOCKED = "Dir fehlt noch das Wissen für diesen Ort."

    /** Shown in the action bar when a portal the player is locked out of is entered. */
    const val PORTAL_LOCKED = "Dieser Weg ist dir noch verschlossen."

    /**
     * Returns whether gates do not apply to [player]: in creative or spectator mode or with the bypass permission.
     *
     * @param player the player
     */
    fun isExempt(player: Player): Boolean =
        GatePlan.isExempt(player.gameMode, player.hasPermission(HubPermissions.GATE_BYPASS))

    /**
     * Returns whether [gate] locks [player] out. Without loaded progression a gate counts as locked; exempt players
     * are never locked out.
     *
     * @param player the player
     * @param gate the gate, or `null` for none
     */
    fun isLockedOut(player: Player, gate: Requirement?): Boolean {
        if (gate == null || isExempt(player)) return false
        val context = Progression.runtimeOrNull?.store?.get(player.uniqueId)?.let { PlayerRequirementContext(player, it) }
        return GatePlan.isLocked(gate, context)
    }

    /**
     * Returns whether the gate of [region] locks [player] out.
     *
     * @param player the player
     * @param region the region, or `null` outside every region
     */
    fun isLockedOut(player: Player, region: Region?): Boolean = region != null && isLockedOut(player, region.gate)

    /**
     * Returns the region covering [location], or `null` outside the hub world or every region.
     *
     * @param location the location to look up
     */
    fun regionAt(location: Location): Region? {
        val hub = HubPlugin.instance
        if (location.world != hub.hubWorld) return null
        return hub.regionIndex.regionAt(location.blockX, location.blockY, location.blockZ)
    }

    /**
     * Shows [player] in the action bar that a region is locked to them.
     *
     * @param player the player
     */
    fun tellRegionLocked(player: Player) {
        player.sendActionBar(buildText { error(REGION_LOCKED) })
    }

    /**
     * Shows [player] in the action bar that a portal is locked to them.
     *
     * @param player the player
     */
    fun tellPortalLocked(player: Player) {
        player.sendActionBar(buildText { error(PORTAL_LOCKED) })
    }
}
