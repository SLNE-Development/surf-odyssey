package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.progression.RegionProgress
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import dev.slne.surf.odyssey.runtime.paper.progression.quest.ProgressEvent
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ToolItems
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

/**
 * Reports mobs killed by players in the hub world to the quest engine, with the region the mob died in. A kill inside
 * a mob spawn region also adds one use to the sword or bow the killer holds in the main hand.
 */
object MobKillListener : Listener {
    /** Reports the kill of a non-player entity by a player. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (entity is Player) return
        val killer = entity.killer ?: return
        val hub = HubPlugin.instance
        val location = entity.location
        if (location.world != hub.hubWorld) return
        val region = hub.regionIndex.regionAt(location.blockX, location.blockY, location.blockZ)
        val weapon = if (region is MobSpawnRegion) weaponOf(killer) else null
        RegionProgress.killed(killer, ProgressEvent.Killed(entity.type, region?.id), weapon)
    }

    /** Returns the type of the progression sword or bow [player] holds in the main hand, or `null` if none. */
    private fun weaponOf(player: Player): ToolType? {
        val state = ToolItems.read(player.inventory.itemInMainHand) ?: return null
        return ToolItems.definitionOf(state)?.type?.takeIf { it == ToolType.SWORD || it == ToolType.BOW }
    }
}
