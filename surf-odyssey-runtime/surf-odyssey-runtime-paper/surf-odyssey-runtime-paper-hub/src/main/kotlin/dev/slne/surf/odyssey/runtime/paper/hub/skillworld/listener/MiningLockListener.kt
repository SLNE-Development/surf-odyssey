package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.listener

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillLocks
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorld
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorldRules
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object MiningLockListener : Listener {
    private val ejecting: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        if (!event.hasChangedBlock()) return
        val to = event.to
        if (!SkillWorld.isSkillWorld(to.world)) return

        val inRegion = SkillWorld.regions.allows(SkillType.MINING, to.blockX, to.blockY, to.blockZ)
        if (!SkillWorldRules.mustEjectFromLockedRegion(SkillLocks.isLocked(SkillType.MINING), inRegion)) return

        val player = event.player
        if (!ejecting.add(player.uniqueId)) return
        player.teleportAsync(SkillWorld.hubSpawn()).whenComplete { _, _ -> ejecting.remove(player.uniqueId) }
        player.sendText {
            appendErrorPrefix()
            error("Die Mining-Höhle wird gerade neu generiert.")
        }
    }
}
