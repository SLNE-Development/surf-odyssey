package dev.slne.surf.odyssey.runtime.paper.content.quests

import dev.slne.surf.odyssey.runtime.paper.content.Items
import dev.slne.surf.odyssey.runtime.paper.content.Npcs
import dev.slne.surf.odyssey.runtime.paper.content.Recipes
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.AnswerKind
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.NodeAction
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.quest
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import net.kyori.adventure.text.Component.text

/**
 * Defines the side quest of Koch Anselm: cook one fish soup at the cooking station and turn it in at him for
 * 3 sushi. His nodes can open the cooking station directly.
 */
internal fun kochFirstDish(): QuestDefinition = quest("koch_first_dish", QuestType.SIDE, Npcs.KOCH) {
    title = text("Das erste Gericht")
    description = text("Koch Anselm will sehen, ob du kochen kannst.")
    offerNode = "k1"

    step(1, Npcs.KOCH, "k3") {
        description = text("Koche eine Fischsuppe")
        craft(Recipes.FISH_SOUP, 1)
    }

    reward(ItemReward(Items.SUSHI.ref, 3))

    node("k1", Npcs.KOCH) {
        say("Neu am Hafen? Dann zeig mal, was du kannst. Koch mir eine Fischsuppe: zwei Kabeljau, ein Streifen Kelp.")
        reply("Ich koche dir eine.", "Quest annehmen", NodeAction.AcceptQuest("k2"), AnswerKind.ACCEPT)
        leave("Heute nicht.")
    }
    node("k2", Npcs.KOCH) {
        mood = "happy"
        say("Die Kochstelle steht gleich hier. Und lass sie nicht anbrennen!")
        reply("Zur Kochstelle", "Station öffnen", NodeAction.OpenStation)
        leave("Bis gleich.")
    }
    node("k3", Npcs.KOCH) {
        say("Und? Riecht es schon nach was?")
        reply("Die Suppe ist fertig.", "Abschließen", NodeAction.TurnIn("k4"))
        reply("Zur Kochstelle", "Station öffnen", NodeAction.OpenStation)
        leave("Gleich.")
    }
    node("k4", Npcs.KOCH) {
        mood = "happy"
        say("Nicht schlecht für den ersten Versuch! Hier, drei Sushi. Davon wirst du satt.")
        leave("Danke!")
    }
}
