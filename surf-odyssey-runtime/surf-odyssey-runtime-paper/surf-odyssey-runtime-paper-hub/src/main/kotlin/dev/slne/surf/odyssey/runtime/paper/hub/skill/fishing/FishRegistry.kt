package dev.slne.surf.odyssey.runtime.paper.hub.skill.fishing

import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.api.core.util.mutableObjectSetOf
import org.bukkit.entity.Fish

object FishRegistry {
    private val _fish = mutableObjectSetOf<Fish>()
    val fish get() = _fish.freeze()

    fun registerFish(fish: Fish) {
        _fish.add(fish)
    }
}