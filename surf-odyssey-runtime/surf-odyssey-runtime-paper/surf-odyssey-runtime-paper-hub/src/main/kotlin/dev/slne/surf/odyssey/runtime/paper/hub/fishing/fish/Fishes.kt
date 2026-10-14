package dev.slne.surf.odyssey.runtime.paper.hub.fishing.fish

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.util.random
import dev.slne.surf.odyssey.core.common.utils.odysseyKey
import dev.slne.surf.odyssey.runtime.paper.common.utils.WeatherType
import dev.slne.surf.odyssey.runtime.paper.hub.fishing.FishableCondition
import org.bukkit.Location

/**
 * Every fish that can be caught. [weight] is the fish's share of the catches among all fish whose conditions are met
 * at the hook, so a weight of 20 next to a total of 100 is caught about a fifth of the time.
 */
enum class Fishes(
    val fish: FishableFish,
    val weight: Int
) {
    DEFAULT_FISH(fishableFish(odysseyKey("default_fish"), {
        primary("Fisch")
    }, {}, key("minecraft:cod")), weight = 70),

    MOONLIGHT_FISH(fishableFish(odysseyKey("moonlight_fish"), {
        primary("Mondbeter")
    }, {

    }, key("minecraft:cod")) {
        // Night, from dusk to just before sunrise
        timeCondition = FishableCondition.TIME_RANGE(13000L..23000L, null)
    }, weight = 20),

    RAINY_FISH(fishableFish(odysseyKey("rainy_fish"), {
        primary("Regenfisch")
    }, {

    }, key("minecraft:cod")) {
        weatherCondition = FishableCondition.WEATHER(WeatherType.RAIN, null)
    }, weight = 20);

    companion object {
        /** Picks a fish by weight among the default fish and every fish whose conditions are met at [location]. */
        fun randomFish(location: Location): FishableFish {
            val pool = entries.filter { it == DEFAULT_FISH || it.fish.meetsConditions(location) }
            var roll = random.nextInt(pool.sumOf { it.weight })

            for (entry in pool) {
                roll -= entry.weight
                if (roll < 0) return entry.fish
            }

            return DEFAULT_FISH.fish
        }
    }
}
