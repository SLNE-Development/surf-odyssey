package dev.slne.surf.odyssey.runtime.paper.hub.skill.fishing.fish

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.rarity.Rarity
import dev.slne.surf.api.paper.builder.LoreBuilder
import dev.slne.surf.odyssey.runtime.paper.hub.skill.fishing.FishableCondition
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.ComponentLike
import org.bukkit.Location
import org.bukkit.inventory.ItemStack

interface Fishable : ComponentLike {
    val key: Key
    val displayName: SurfComponentBuilder.() -> Unit
    val description: LoreBuilder.() -> Unit
    val modelKey: Key

    fun buildItemStack(rarity: Rarity): ItemStack

    var timeCondition: FishableCondition
    var temperatureCondition: FishableCondition
    var humidityCondition: FishableCondition
    var weatherCondition: FishableCondition
    var waterDepthCondition: FishableCondition

    var locationCondition: FishableCondition
    var biomeCondition: FishableCondition

    override fun asComponent() = SurfComponentBuilder.builder().apply(displayName).build()

    fun meetsConditions(location: Location): Boolean {
        val conditionHelper = FishableCondition.ConditionHelper.fromLocation(location)

        val timeConditionMet = timeCondition.isMet(conditionHelper)
        val temperatureConditionMet = temperatureCondition.isMet(conditionHelper)
        val humidityConditionMet = humidityCondition.isMet(conditionHelper)
        val weatherConditionMet = weatherCondition.isMet(conditionHelper)
        val waterDepthConditionMet = waterDepthCondition.isMet(conditionHelper)
        val locationConditionMet = locationCondition.isMet(conditionHelper)
        val biomeConditionMet = biomeCondition.isMet(conditionHelper)

        return timeConditionMet && temperatureConditionMet && humidityConditionMet &&
                weatherConditionMet && waterDepthConditionMet && locationConditionMet && biomeConditionMet
    }

}