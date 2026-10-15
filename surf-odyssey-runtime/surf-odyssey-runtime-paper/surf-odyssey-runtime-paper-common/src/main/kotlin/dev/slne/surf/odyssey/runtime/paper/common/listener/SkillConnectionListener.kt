package dev.slne.surf.odyssey.runtime.paper.common.listener

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.odyssey.core.common.player.PlayerManager
import dev.slne.surf.odyssey.core.common.skill.SkillBridge
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import dev.slne.surf.odyssey.runtime.paper.common.skill.SkillRuntime
import dev.slne.surf.odyssey.runtime.paper.common.skill.forgetLevelUpDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.logging.Level

object SkillConnectionListener : Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    fun onJoin(event: PlayerJoinEvent) {
        val uuid = event.player.uniqueId

        plugin.launch {
            runCatching { SkillBridge.loadSkills(uuid) }
                .onSuccess { PlayerManager.cachedPlayer(uuid)?.applySkills(it) }
                .onFailure {
                    plugin.logger.log(
                        Level.SEVERE,
                        "Failed to load skills of $uuid, skills stay disabled until the next login",
                        it
                    )
                }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        val snapshot = SkillRuntime.scheduler.snapshot(uuid)
        SkillRuntime.scheduler.forget(uuid)
        forgetLevelUpDisplay(uuid)

        if (snapshot.isEmpty()) return

        SkillRuntime.persistAsync(uuid, snapshot)
    }
}