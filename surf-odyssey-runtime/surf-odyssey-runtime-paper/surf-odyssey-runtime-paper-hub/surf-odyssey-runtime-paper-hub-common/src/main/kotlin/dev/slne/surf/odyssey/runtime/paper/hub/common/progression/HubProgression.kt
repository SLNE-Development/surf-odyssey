package dev.slne.surf.odyssey.runtime.paper.hub.common.progression

import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMode
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionPlacements
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentPack
import org.bukkit.plugin.Plugin

/** Runs the progression engine of a hub world with every feature turned on. */
object HubProgression {
    /**
     * Installs the progression engine in [ProgressionMode.FULL] with [content] and [placements]. The server name sent
     * with tool sightings comes from surf-core.
     *
     * @param plugin the hub plugin
     * @param content the progression content of the network
     * @param placements where the NPCs and stations of this world stand
     * @throws dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentValidationException if
     *         the content is invalid
     */
    fun install(plugin: Plugin, content: ContentPack, placements: ProgressionPlacements) {
        ProgressionModule.install(
            plugin,
            content,
            ProgressionMode.FULL,
            serverName = SurfCoreApi::getCurrentServerName,
            placements = placements,
        )
    }

    /** Despawns the NPCs and sends the buffered progress of the installed engine; does nothing without one. */
    suspend fun shutdown() {
        Progression.runtimeOrNull?.shutdown()
    }
}
