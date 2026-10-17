package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.hud

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.hud.api.HudElement
import dev.slne.surf.hud.api.SurfHud
import dev.slne.surf.hud.api.hud
import dev.slne.surf.odyssey.runtime.paper.hub.plugin
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorld
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object RegionHud : Listener {
    private const val ELEMENT_ID = "region"

    private const val PERIOD_TICKS = 10L

    private val shown = ConcurrentHashMap<UUID, RegionLabel>()

    private val tasks = ConcurrentHashMap<UUID, ScheduledTask>()

    fun startForOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach(::start)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        start(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        stop(event.player.uniqueId)
        SurfHud.remove(event.player.uniqueId)
    }

    private fun start(player: Player) {
        tasks.remove(player.uniqueId)?.cancel()
        shown.remove(player.uniqueId)
        val task = player.scheduler.runAtFixedRate(
            plugin,
            { update(player) },
            { stop(player.uniqueId) },
            1L,
            PERIOD_TICKS,
        ) ?: return
        tasks[player.uniqueId] = task
    }

    private fun stop(uuid: UUID) {
        tasks.remove(uuid)?.cancel()
        shown.remove(uuid)
    }

    private fun update(player: Player) {
        val location = player.location
        val label = resolveRegionLabel(
            worldKind(location.world),
            SkillWorld.regions,
            location.blockX,
            location.blockY,
            location.blockZ,
        )
        if (shown[player.uniqueId] == label) return

        if (label == null) {
            player.hud.remove(ELEMENT_ID)
            shown.remove(player.uniqueId)
            return
        }

        player.hud.add(HudElement(ELEMENT_ID, 0, 0, buildText { primary(label.text) }))
        shown[player.uniqueId] = label
        WelcomeTitles.greet(player, label)
    }

    private fun worldKind(world: World): WorldKind = when {
        world == SkillWorld.hubWorld() -> WorldKind.HUB
        SkillWorld.isSkillWorld(world) -> WorldKind.SKILL_WORLD
        else -> WorldKind.OTHER
    }
}
