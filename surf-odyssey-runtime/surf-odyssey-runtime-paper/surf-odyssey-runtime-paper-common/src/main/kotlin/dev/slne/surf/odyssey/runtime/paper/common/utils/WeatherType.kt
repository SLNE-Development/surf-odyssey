package dev.slne.surf.odyssey.runtime.paper.common.utils

import org.bukkit.Location

enum class WeatherType {

    CLEAR,
    RAIN,
    THUNDER;

    companion object {
        fun fromLocation(location: Location): WeatherType {
            val world = location.world

            val hasStorm = world.hasStorm()
            val isThundering = world.isThundering

            return when {
                isThundering -> THUNDER
                hasStorm -> RAIN
                else -> CLEAR
            }
        }
    }

}