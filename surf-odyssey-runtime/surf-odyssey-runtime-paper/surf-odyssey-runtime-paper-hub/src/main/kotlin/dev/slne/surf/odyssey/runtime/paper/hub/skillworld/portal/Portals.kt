package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal

import dev.slne.surf.odyssey.runtime.paper.hub.plugin
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.SkillWorldSettings
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockBox

object Portals {
    @Volatile
    var all: List<Portal> = emptyList()
        private set

    fun refresh() {
        all = SkillWorldSettings.config.portals.mapNotNull { (id, config) ->
            runCatching { Portal(id, config.kind, config.skill, BlockBox.of(config.corner1.toPos(), config.corner2.toPos())) }
                .onFailure { plugin.logger.warning("Skipping invalid portal $id in skill-world.yml: ${it.message}") }
                .getOrNull()
        }
    }
}
