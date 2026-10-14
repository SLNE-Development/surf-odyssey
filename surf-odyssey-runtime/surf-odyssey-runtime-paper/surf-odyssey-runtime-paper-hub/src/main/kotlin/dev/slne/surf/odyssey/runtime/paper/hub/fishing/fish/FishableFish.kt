@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.hub.fishing.fish

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.rarity.Rarity
import dev.slne.surf.api.paper.builder.LoreBuilder
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.odyssey.runtime.paper.hub.fishing.FishableCondition
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.key.Key
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

fun fishableFish(
    key: Key,
    displayName: SurfComponentBuilder.() -> Unit,
    lore: LoreBuilder.() -> Unit = {
        line {
            spacer("Blubb")
        }
    },
    modelKey: Key,
    init: (FishableFish.() -> Unit)? = null
): FishableFish {
    return object : FishableFish(key, displayName, lore, modelKey) {
        init {
            init?.invoke(this)
        }
    }
}

abstract class FishableFish(
    override val key: Key,
    override val displayName: SurfComponentBuilder.() -> Unit,
    override val description: LoreBuilder.() -> Unit = {
        line { spacer("Blubb") }
    },
    override val modelKey: Key
) : Fishable {
    override fun buildItemStack(
        rarity: Rarity
    ): ItemStack = buildItem(ItemType.PAPER, 1) {
        setData(DataComponentTypes.ITEM_MODEL, modelKey)

        this.displayName(displayName)

        buildLore {
            emptyLine()
            description()
            emptyLine()
            line {
                spacer("-".repeat(10))
            }
            emptyLine()
            line {
                variableKey("Seltenheit: ")
                append(rarity)
            }
            emptyLine()
        }
    }

    override var timeCondition: FishableCondition = FishableCondition.ALWAYS
    override var temperatureCondition: FishableCondition = FishableCondition.ALWAYS
    override var humidityCondition: FishableCondition = FishableCondition.ALWAYS
    override var weatherCondition: FishableCondition = FishableCondition.ALWAYS
    override var waterDepthCondition: FishableCondition = FishableCondition.ALWAYS

    override var locationCondition: FishableCondition = FishableCondition.ALWAYS
    override var biomeCondition: FishableCondition = FishableCondition.ALWAYS

}