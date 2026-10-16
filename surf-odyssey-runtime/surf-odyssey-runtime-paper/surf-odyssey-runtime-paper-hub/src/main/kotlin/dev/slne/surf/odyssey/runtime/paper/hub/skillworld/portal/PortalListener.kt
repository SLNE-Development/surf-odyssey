package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillLocks
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorld
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.SkillWorldSettings
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PortalListener : Listener {
    private const val COOLDOWN_MILLIS = 2_000L

    private val cooldownUntil = ConcurrentHashMap<UUID, Long>()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        if (!event.hasChangedBlock()) return

        val to = event.to
        val world = to.world
        val portals = Portals.all.filter { it.world() == world }
        if (portals.isEmpty()) return

        val cells = PortalBlocks.bodyCells(to.blockX, to.blockY, to.blockZ)
        val portal = PortalBlocks.triggers(portals, cells) { x, y, z -> NexoPortalBlocks.isPortalBlock(world.getBlockAt(x, y, z)) } ?: return

        val player = event.player
        val now = System.currentTimeMillis()
        if (now < (cooldownUntil[player.uniqueId] ?: 0L)) return
        cooldownUntil[player.uniqueId] = now + COOLDOWN_MILLIS

        val outcome = PortalRouter.route(
            portal.kind,
            portal.skill,
            SkillWorld.world != null,
            SkillLocks.snapshot(),
            SkillWorldSettings.config.arrivals.keys,
        )
        handle(player, outcome)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        cooldownUntil.remove(event.player.uniqueId)
    }

    private fun handle(player: Player, outcome: PortalOutcome) {
        when (outcome) {
            is PortalOutcome.Teleport -> target(outcome.target)?.let { player.teleportAsync(it) }
            PortalOutcome.WorldUnavailable -> player.sendText {
                appendErrorPrefix()
                error("Die Skill-Welt ist gerade nicht verfügbar.")
            }
            PortalOutcome.SkillLocked -> player.sendText {
                appendErrorPrefix()
                error("Dieser Bereich wird gerade neu generiert. Versuche es gleich noch einmal.")
            }
            PortalOutcome.NoArrival -> player.sendText {
                appendErrorPrefix()
                error("Dieses Portal ist noch nicht eingerichtet.")
            }
        }
    }

    private fun target(target: PortalTarget): Location? = when (target) {
        PortalTarget.HubSpawn -> SkillWorld.hubSpawn()
        is PortalTarget.Arrival -> {
            val world = SkillWorld.world
            val arrival = SkillWorldSettings.config.arrivals[target.skill]
            if (world == null || arrival == null) null
            else Location(world, arrival.x, arrival.y, arrival.z, arrival.yaw, arrival.pitch)
        }
    }
}
