package dev.slne.surf.odyssey.runtime.paper.hub.fishing.fish

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.util.mutableObjectSetOf
import dev.slne.surf.odyssey.core.common.utils.odysseyKey
import dev.slne.surf.odyssey.runtime.paper.common.utils.WeatherType
import dev.slne.surf.odyssey.runtime.paper.hub.fishing.FishableCondition
import org.bukkit.Location

enum class Fishes(
    val fish: FishableFish
) {
    DEFAULT_FISH(fishableFish(odysseyKey("default_fish"), {
        primary("Fisch")
    }, {}, key("minecraft:cod"))),

    MOONLIGHT_FISH(fishableFish(odysseyKey("moonlight_fish"), {
        primary("Mondbeter")
    }, {

    }, key("minecraft:cod")) {
        timeCondition = FishableCondition.TIME_RANGE(12000L..1000L, false)
    }),

    RAINY_FISH(fishableFish(odysseyKey("rainy_fish"), {
        primary("Regenfisch")
    }, {

    }, key("minecraft:cod")) {
        weatherCondition = FishableCondition.WEATHER(WeatherType.RAIN, null)
    });

    companion object {
        fun randomFish(location: Location): FishableFish {
            val meetsConditions = mutableObjectSetOf<Fishes>()

            entries.filterNot { it == DEFAULT_FISH }.forEach { fish ->
                if (fish.fish.meetsConditions(location)) {
                    meetsConditions.add(fish)
                }
            }

            return if (meetsConditions.isNotEmpty()) {
                meetsConditions.random().fish
            } else {
                DEFAULT_FISH.fish
            }
        }
    }
}