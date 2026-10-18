package dev.slne.surf.odyssey.runtime.paper.content

import dev.slne.surf.odyssey.runtime.paper.content.quests.Quests
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcBuilder
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.npc
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirements
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.Price
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeResult
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component.text
import org.bukkit.Material

/** The NPCs of the Odyssey content. Where they stand is set by the world plugins that place them. */
object Npcs {
    /** Brina, the harbour master in the lobby, who gives the lighthouse questline. */
    val BRINA: NpcDefinition = npc("brina") {
        name = text("Brina")
        skin = NpcSkins.BRINA
        portraits("brina", "neutral", "worried", "happy")
        greeting = text("Moin. Ohne Leuchtfeuer ist dieser Hafen nur ein Ort, an dem Schiffe zerbrechen.")
    }

    /**
     * Koch Anselm in the lobby, who runs the cooking station and sells the sushi and smoked fish recipes, bait worms
     * and a fish platter for coins.
     */
    val KOCH: NpcDefinition = npc("koch") {
        name = text("Koch Anselm")
        skin = NpcSkins.KOCH
        portraits("koch", "neutral", "happy")
        greeting = text("Hunger? Dann bist du hier richtig. Und wenn nicht, kommt der noch.")
        station = Stations.COOKING_STATION
        trade("sushi_recipe", TradeResult.Recipe(Recipes.SUSHI), items(Material.COD, 6))
        trade("smoked_fish_recipe", TradeResult.Recipe(Recipes.SMOKED_FISH), items(Material.SALMON, 4))
        trade("bait_worms", TradeResult.Item(Items.BAIT_WORM.ref, 4), items(Material.COD, 3))
        trade("fish_platter", TradeResult.Item(Items.FISH_PLATTER.ref, 1), Price.Currency("coins", 25.0))
    }

    /** Olek, the old fisherman at the North lake. */
    val OLEK: NpcDefinition = npc("olek") {
        name = text("Olek")
        skin = NpcSkins.OLEK
        portraits("olek", "neutral", "happy")
        greeting = text("Na, Landratte? Der Steg knarzt, aber er hält. Meistens.")
    }

    /** Mira, the cartographer in North. */
    val MIRA: NpcDefinition = npc("mira") {
        name = text("Mira")
        skin = NpcSkins.MIRA
        portraits("mira", "neutral", "thinking")
        greeting = text("Vorsicht mit den Karten, die Tinte ist noch feucht.")
    }

    /** Ysa, the tide caller in South, visible once the lighthouse questline has reached its third step. */
    val YSA: NpcDefinition = npc("ysa") {
        name = text("Ysa")
        skin = NpcSkins.YSA
        portraits("ysa", "neutral", "mystic")
        greeting = text("Das Meer hat mir gesagt, dass du kommst.")
        visibleWhen = Requirements.questStepReached(Quests.LEUCHTFEUER_ID, 2)
    }

    /** All NPCs in the order they are registered. */
    val all: List<NpcDefinition> = listOf(BRINA, KOCH, OLEK, MIRA, YSA)

    /**
     * Adds the portraits `odyssey:portrait_<npc>_<mood>` for every mood in [moods].
     *
     * @param npc the NPC ID used in the image IDs
     * @param moods the moods with a portrait
     */
    private fun NpcBuilder.portraits(npc: String, vararg moods: String) {
        moods.forEach { portrait(it, Key.key("odyssey", "portrait_${npc}_$it")) }
    }

    /**
     * Returns a price of [amount] plain items of [material].
     *
     * @param material the paid material
     * @param amount how many are paid
     */
    private fun items(material: Material, amount: Int): Price = Price.Items(listOf(Ingredient(material.ref, amount)))
}
