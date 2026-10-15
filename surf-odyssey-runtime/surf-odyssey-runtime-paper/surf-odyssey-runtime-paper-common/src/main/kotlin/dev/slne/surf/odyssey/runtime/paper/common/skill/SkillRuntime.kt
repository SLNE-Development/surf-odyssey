package dev.slne.surf.odyssey.runtime.paper.common.skill

import dev.slne.surf.odyssey.core.common.player.PlayerManager
import dev.slne.surf.odyssey.core.common.skill.SkillBridge
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.logging.Level

internal object SkillRuntime {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val scheduler = SkillSaveScheduler(
        scope = scope,
        debounce = SkillSettings.saveDebounce,
        interval = SkillSettings.saveInterval,
        xpOf = { uuid, skill -> PlayerManager.cachedPlayer(uuid)?.xp(skill) ?: 0L },
        save = { uuid, skill, xp -> SkillBridge.saveSkill(uuid, skill, xp) },
        onFailure = { uuid, skill, cause ->
            plugin.logger.log(Level.SEVERE, "Failed to save $skill experience of $uuid", cause)
        }
    )

    fun start() {
        scheduler.start()
    }

    fun persistAsync(uuid: UUID, snapshot: Map<SkillType, Long>) {
        scope.launch { scheduler.persist(uuid, snapshot) }
    }

    suspend fun stop() {
        scheduler.stop()
        scheduler.flushAll()
        scope.coroutineContext.job.children.toList().joinAll()
        scope.cancel()
    }
}