package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.listener

import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorld
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.command.Selections
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent

object SkillWorldPlayerListener : Listener {
    @EventHandler(priority = EventPriority.HIGH)
    fun onSpawnLocation(event: AsyncPlayerSpawnLocationEvent) {
        val world = event.spawnLocation.world ?: return
        if (SkillWorld.isEditWorld(world)) event.spawnLocation = SkillWorld.hubSpawn()
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onRespawn(event: PlayerRespawnEvent) {
        if (SkillWorld.isSkillWorld(event.player.world)) event.respawnLocation = SkillWorld.hubSpawn()
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        Selections.forget(event.player.uniqueId)
    }
}
