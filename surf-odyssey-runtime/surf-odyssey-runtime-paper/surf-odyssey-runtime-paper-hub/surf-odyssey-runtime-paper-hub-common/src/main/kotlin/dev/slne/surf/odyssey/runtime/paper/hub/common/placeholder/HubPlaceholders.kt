package dev.slne.surf.odyssey.runtime.paper.hub.common.placeholder

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.PlayerRegionTracker
import io.github.miniplaceholders.api.Expansion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import org.bukkit.entity.Player

/**
 * MiniPlaceholders tags of a hub world: `<odyssey_world>` names the world,
 * `<odyssey_region_name>` the player's region (empty outside), `<odyssey_region>` both joined.
 */
object HubPlaceholders {
    /** The registered expansion, or `null` while the tags are not registered. */
    private var expansion: Expansion? = null

    /** Registers the tags for [hub]. */
    fun register(hub: HubPlugin) {
        expansion = Expansion.builder("odyssey")
            .audiencePlaceholder(Player::class.java, "world") { _, _, _ -> Tag.selfClosingInserting(hub.worldDisplayName) }
            .audiencePlaceholder(Player::class.java, "region_name") { player, _, _ ->
                Tag.selfClosingInserting(PlayerRegionTracker.regionOf(player)?.displayName ?: Component.empty())
            }
            .audiencePlaceholder(Player::class.java, "region") { player, _, _ ->
                Tag.selfClosingInserting(RegionDisplay.combined(hub.worldDisplayName, PlayerRegionTracker.regionOf(player)?.displayName))
            }
            .build()
            .also { it.register() }
    }

    /** Removes the tags. */
    fun unregister() {
        expansion?.unregister()
        expansion = null
    }
}
