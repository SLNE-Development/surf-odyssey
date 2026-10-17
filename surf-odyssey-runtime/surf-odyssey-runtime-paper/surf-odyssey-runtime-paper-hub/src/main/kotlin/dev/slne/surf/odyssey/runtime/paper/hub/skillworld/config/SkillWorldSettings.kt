package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config

import dev.slne.surf.api.core.config.manager.SpongeConfigManager
import dev.slne.surf.api.core.config.surfConfigApi
import dev.slne.surf.odyssey.runtime.paper.hub.plugin

object SkillWorldSettings {
    private var manager: SpongeConfigManager<SkillWorldConfig>? = null

    val isLoaded: Boolean get() = manager != null

    val config: SkillWorldConfig get() = checkNotNull(manager) { "skill-world.yml has not been loaded" }.config

    fun load() {
        manager = surfConfigApi.createSpongeYmlConfigManager(SkillWorldConfig::class.java, plugin.dataPath, "skill-world.yml")
    }

    fun edit(transform: (SkillWorldConfig) -> SkillWorldConfig) {
        val current = checkNotNull(manager) { "skill-world.yml has not been loaded" }
        current.config = transform(current.config)
        current.save()
    }

    fun reload(): SkillWorldConfig {
        val current = manager ?: return run {
            load()
            config
        }
        return current.reloadFromFile()
    }
}
