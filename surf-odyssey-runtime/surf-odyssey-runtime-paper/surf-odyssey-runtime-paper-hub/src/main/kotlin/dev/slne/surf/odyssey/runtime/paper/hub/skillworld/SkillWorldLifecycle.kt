package dev.slne.surf.odyssey.runtime.paper.hub.skillworld

import org.bukkit.World

interface SkillWorldLifecycle {
    fun onLoaded(world: World) {}

    fun onUnloading(world: World) {}
}
