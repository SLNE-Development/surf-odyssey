package dev.slne.surf.odyssey.runtime.paper.common.skill

import dev.slne.surf.odyssey.runtime.paper.common.plugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal object SkillSettings {
    val saveDebounce: Duration
        get() = plugin.config.getLong("skills.save-debounce-seconds", 5L).coerceAtLeast(1L).seconds

    val saveInterval: Duration
        get() = plugin.config.getLong("skills.save-interval-seconds", 300L).coerceAtLeast(1L).seconds
}