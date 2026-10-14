package dev.slne.surf.odyssey.runtime.paper.hub.fishing

import dev.slne.surf.odyssey.runtime.paper.common.utils.WeatherType
import it.unimi.dsi.fastutil.objects.ObjectSet
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Biome

fun interface FishableCondition {

    companion object {
        val TEMPERATURE: (Double) -> FishableCondition = { targetTemperature ->
            FishableCondition { _, _, temperature, _, _, _, _ ->
                temperature == targetTemperature
            }
        }

        val TEMPERATURE_RANGE: (ClosedRange<Double>) -> FishableCondition = { range ->
            FishableCondition { _, _, temperature, _, _, _, _ ->
                temperature in range
            }
        }

        val HUMIDITY: (Double) -> FishableCondition = { targetHumidity ->
            FishableCondition { _, _, _, humidity, _, _, _ ->
                humidity == targetHumidity
            }
        }

        val HUMIDITY_RANGE: (ClosedRange<Double>) -> FishableCondition = { range ->
            FishableCondition { _, _, _, humidity, _, _, _ ->
                humidity in range
            }
        }

        val BIOME: (Biome) -> FishableCondition = { targetBiome ->
            FishableCondition { _, biome, _, _, _, _, _ ->
                biome == targetBiome
            }
        }

        val BIOME_SET: (ObjectSet<Biome>) -> FishableCondition = { targetBiomes ->
            FishableCondition { _, biome, _, _, _, _, _ ->
                targetBiomes.contains(biome)
            }
        }

        val WEATHER: (WeatherType, Int?) -> FishableCondition =
            { targetWeather, minWeatherDuration ->
                FishableCondition { _, _, _, _, _, weather, _ ->
                    weather.first == targetWeather &&
                            (minWeatherDuration == null || weather.second >= minWeatherDuration)
                }
            }

        val WEATHER_SET: (Set<WeatherType>, Int?) -> FishableCondition =
            { targetWeathers, minWeatherDuration ->
                FishableCondition { _, _, _, _, _, weather, _ ->
                    targetWeathers.contains(weather.first) &&
                            (minWeatherDuration == null || weather.second >= minWeatherDuration)
                }
            }

        val WATER_DEPTH: (Int) -> FishableCondition = { targetDepth ->
            FishableCondition { _, _, _, _, _, _, waterDepth ->
                waterDepth >= targetDepth
            }
        }

        val WATER_DEPTH_RANGE: (IntRange) -> FishableCondition = { range ->
            FishableCondition { _, _, _, _, _, _, waterDepth ->
                waterDepth in range
            }
        }

        val TIME: (Long, Boolean?) -> FishableCondition = { targetTime, isDay ->
            FishableCondition { _, _, _, _, time, _, _ ->
                time.first == targetTime && (isDay == null || time.second == isDay)
            }
        }

        val TIME_RANGE: (LongRange, Boolean?) -> FishableCondition = { range, isDay ->
            FishableCondition { _, _, _, _, time, _, _ ->
                val start = range.first
                val end = range.last
                val currentTime = time.first

                val inRange = if (start <= end) {
                    currentTime in start..end
                } else {
                    currentTime in start..23999L || currentTime in 0L..end
                }

                inRange && (isDay == null || time.second == isDay)
            }
        }

        val ALWAYS: FishableCondition = FishableCondition { _, _, _, _, _, _, _ -> true }
    }

    /**
     * Checks if the fishable condition is met based on the provided parameters.
     *
     * @param location The location where the condition is being checked.
     * @param biome The biome at the specified location.
     * @param temperature The temperature at the specified location.
     * @param humidity The humidity at the specified location.
     * @param time A pair containing the current time in ticks and whether it is day or night.
     * @param weather A pair containing the current weather type and its duration.
     * @param waterDepth The depth of the water at the specified location.
     *
     * @return True if the condition is met, false otherwise.
     */
    fun isMet(
        location: Location,
        biome: Biome,
        temperature: Double,
        humidity: Double,
        time: Pair<Long, Boolean>,
        weather: Pair<WeatherType, Long>,
        waterDepth: Int,
    ): Boolean

    /**
     * Checks if the fishable condition is met using a helper object that contains all necessary parameters.
     *
     * @param helper A helper object containing the location, biome, temperature, humidity, time, weather, and water depth.
     *
     * @return True if the condition is met, false otherwise.
     */
    fun isMet(
        helper: ConditionHelper
    ): Boolean = isMet(
        location = helper.location,
        biome = helper.biome,
        temperature = helper.temperature,
        humidity = helper.humidity,
        time = helper.time,
        weather = helper.weather,
        waterDepth = helper.waterDepth
    )

    /**
     * A helper data class that contains all the necessary parameters to check if a fishable condition is met.
     *
     * @param location The location where the condition is being checked.
     * @param biome The biome at the specified location.
     * @param temperature The temperature at the specified location.
     * @param humidity The humidity at the specified location.
     * @param time A pair containing the current time in ticks and whether it is day or night.
     * @param weather A pair containing the current weather type and its duration.
     * @param waterDepth The depth of the water at the specified location.
     */
    data class ConditionHelper(
        val location: Location,
        val biome: Biome,
        val temperature: Double,
        val humidity: Double,
        val time: Pair<Long, Boolean>,
        val weather: Pair<WeatherType, Long>,
        val waterDepth: Int
    ) {
        companion object {
            private const val MAX_USABLE_WATER_DEPTH = 255

            fun fromLocation(location: Location): ConditionHelper {
                val world = location.world
                val x = location.blockX
                val y = location.blockY
                val z = location.blockZ

                val biome = world.getBiome(location)
                val temperature = world.getTemperature(x, y, z)
                val humidity = world.getHumidity(x, y, z)
                val time = world.time

                val isDay = world.isDayTime
                val clearWeatherFor = world.clearWeatherDuration.toLong()

                val storming = world.hasStorm()
                val stormingFor = world.weatherDuration.toLong()

                val thundering = world.isThundering
                val thunderingFor = world.thunderDuration.toLong()

                val weatherType = when {
                    storming && thundering -> WeatherType.THUNDER
                    storming -> WeatherType.RAIN
                    else -> WeatherType.CLEAR
                }
                val weatherDuration = when (weatherType) {
                    WeatherType.THUNDER -> thunderingFor
                    WeatherType.RAIN -> stormingFor
                    WeatherType.CLEAR -> clearWeatherFor
                }

                val inWater = location.block.type == Material.WATER
                var waterDepth = 0

                if (inWater) {
                    var currentBlock = location.block

                    while (currentBlock.type == Material.WATER && waterDepth < MAX_USABLE_WATER_DEPTH) {
                        waterDepth++
                        currentBlock = currentBlock.getRelative(0, -1, 0)
                    }
                }

                return ConditionHelper(
                    location = location,
                    biome = biome,
                    temperature = temperature,
                    humidity = humidity,
                    time = Pair(time, isDay),
                    weather = Pair(weatherType, weatherDuration),
                    waterDepth = waterDepth
                )
            }
        }
    }

}