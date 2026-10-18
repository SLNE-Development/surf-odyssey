package dev.slne.surf.odyssey.runtime.paper.content.quests

import dev.slne.surf.odyssey.runtime.paper.content.Items
import dev.slne.surf.odyssey.runtime.paper.content.Npcs
import dev.slne.surf.odyssey.runtime.paper.content.Recipes
import dev.slne.surf.odyssey.runtime.paper.content.Titles
import dev.slne.surf.odyssey.runtime.paper.content.Tools
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.AnswerKind
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.NodeAction
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.quest
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.CurrencyReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.RecipeReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.TitleReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ToolReward
import net.kyori.adventure.text.Component.text
import org.bukkit.Material

/**
 * Defines the lighthouse questline, offered by Brina in four steps:
 *
 * 1. talk to Olek at the pier,
 * 2. catch 5 salmon in the region `north_lake` and turn them in at Olek, which unlocks the bait worm recipe,
 * 3. bring Mira 12 ink sacs,
 * 4. bring Ysa 3 lamp oil.
 *
 * Completing it grants the title "Hüter des Leuchtfeuers", the drill bit recipe, the harbour rod and 200 coins.
 * Every step whose objective is already met offers an answer that turns it in directly, so a conversation that ended
 * early can be picked up again.
 */
internal fun leuchtfeuer(): QuestDefinition = quest(Quests.LEUCHTFEUER_ID, QuestType.MAIN, Npcs.BRINA) {
    title = text("Das erloschene Leuchtfeuer")
    description = text("Das Leuchtfeuer ist dunkel und der Wärter verschwunden. Brina braucht Hilfe.")
    offerNode = "b1"

    step(1, Npcs.OLEK, "o1") {
        description = text("Sprich mit Olek am Steg")
        talkTo(Npcs.OLEK, "o1")
    }
    step(1, Npcs.OLEK, "o5") {
        description = text("Fange 5 Lachse am Nordsee")
        fish(5, material = Material.SALMON, regionId = "north_lake")
        reward(RecipeReward(Recipes.BAIT_WORM))
    }
    step(1, Npcs.MIRA, "m1") {
        description = text("Bring 12 Tintensäcke")
        deliver(Material.INK_SAC.ref, 12, Npcs.MIRA)
    }
    step(1, Npcs.YSA, "y1") {
        description = text("Bring 3 Lampenöl")
        deliver(Items.LANTERN_OIL.ref, 3, Npcs.YSA)
    }

    reward(TitleReward(Titles.LIGHTKEEPER))
    reward(RecipeReward(Recipes.DRILL_BIT))
    reward(ToolReward(Tools.HARBOR_ROD))
    reward(CurrencyReward("coins", 200.0))

    node("b1", Npcs.BRINA) {
        mood = "worried"
        say("Drei Nächte schon ist das Leuchtfeuer dunkel. Zwei Kutter sind auf den Klippen zerschellt. Und der Wärter? Spurlos weg.")
        reply("Was ist mit dem Wärter passiert?", "Nachfragen", NodeAction.Goto("b2"))
        reply("Ich bringe das Feuer wieder zum Brennen.", "Quest annehmen", NodeAction.AcceptQuest("b3"), AnswerKind.ACCEPT)
        leave("Nicht mein Problem.")
    }
    node("b2", Npcs.BRINA) {
        mood = "worried"
        say("Er hat von Lichtern unter dem Wasser geredet. Olek, der alte Fischer am Steg, war der Letzte, der ihn gesehen hat.")
        reply("Ich rede mit Olek.", "Quest annehmen", NodeAction.AcceptQuest("b3"), AnswerKind.ACCEPT)
        reply("Lichter unter Wasser? Klingt nach Seemannsgarn.", "Zurück", NodeAction.Goto("b1"))
    }
    node("b3", Npcs.BRINA) {
        mood = "happy"
        say("Gut. Sprich mit Olek am Steg. Und nimm eine Laterne mit, die Nächte hier sind schwärzer als Tinte.")
        leave("Ich mache mich auf den Weg.")
    }

    node("o1", Npcs.OLEK) {
        say("Der Wärter? Ja. Er ist mit meinem Boot raus, Richtung Seufzerriff. Ohne Karte. Kein Mensch fährt da ohne Karte.")
        reply("Wer hat denn eine Karte?", "Nachfragen", NodeAction.CompleteTalk("o2"))
        reply("Dann fahre ich eben ohne Karte raus.", "Nachfragen", NodeAction.Goto("o3"))
        reply("Du hast mir schon von Mira erzählt.", "Weiter", NodeAction.TurnIn("o4"))
        leave("Ich muss los.")
    }
    node("o3", Npcs.OLEK) {
        say("Ohne Karte? Bestimmt nicht. Das Riff hat schon bessere Seeleute gefressen als dich.")
        reply("Schon gut. Wer hat denn eine Karte?", "Nachfragen", NodeAction.CompleteTalk("o2"))
    }
    node("o2", Npcs.OLEK) {
        say("Mira, die Kartografin oben am Hang. Aber sie gibt nichts umsonst.")
        reply("Dann gehe ich zu Mira.", "Weiter", NodeAction.TurnIn("o4"))
    }
    node("o4", Npcs.OLEK) {
        say("Halt, nicht so schnell. Erst hilfst du mir: Mein Netz ist leer, und meine Knie wollen nicht mehr. Fang mir fünf Lachse, dann zeig ich dir, wie man Köder bindet.")
        leave("Fünf Lachse, kein Problem.")
    }
    node("o5", Npcs.OLEK) {
        say("Und? Beißen sie? Fünf Lachse, nicht einer weniger.")
        reply("Hier sind deine Lachse.", "Abgeben", NodeAction.TurnIn("o6"))
        leave("Ich bin noch dabei.")
    }
    node("o6", Npcs.OLEK) {
        mood = "happy"
        say("Prachtkerle! Pass auf: zwei Halme Weizen, ein Streifen Kelp, und die Fische beißen wie verrückt. Jetzt aber ab zu Mira, oben am Hang.")
        leave("Danke, Olek.")
    }

    node("m1", Npcs.MIRA) {
        mood = "thinking"
        say("Olek schickt dich? Das Riff verschiebt sich mit jeder Flut. Meine Karte ist zwei Wochen alt, also schon fast falsch.")
        reply("Dann zeichne mir eine neue.", "Nachfragen", NodeAction.Goto("m2"))
        reply("Die Tinte hast du schon. Ist die Karte fertig?", "Weiter", NodeAction.TurnIn("m4"))
        leave("Ich komme später wieder.")
    }
    node("m2", Npcs.MIRA) {
        meta = text("Mira braucht 12 Tintensäcke.")
        say("Bring mir die Tinte, dann zeichne ich dir den Weg. Und wenn du da draußen Lichter siehst: nicht folgen.")
        reply("Hier sind zwölf Tintensäcke.", "Abgeben", NodeAction.DeliverItems("m3"))
        leave("Ich besorge die Tinte.")
    }
    node("m3", Npcs.MIRA) {
        mood = "thinking"
        say("Zwölf Säcke, sauber gezählt. Gib mir einen Moment. So: das Riff, die Strömung, und hier unten im Süden wohnt Ysa.")
        reply("Gib mir die Karte.", "Weiter", NodeAction.TurnIn("m4"))
    }
    node("m4", Npcs.MIRA) {
        say("Ysa ist Gezeitenruferin. Wenn jemand weiß, was da unten leuchtet, dann sie. Und denk dran: nicht folgen.")
        leave("Ich gehe nach Süden.")
    }

    node("y1", Npcs.YSA) {
        mood = "mystic"
        say("Du hast die Lichter gesehen? Dann weißt du, warum das Feuer erlosch. Bring mir drei Flaschen Lampenöl, und ich entzünde es neu.")
        reply("Hier ist das Lampenöl.", "Abgeben", NodeAction.DeliverItems("y3"))
        reply("Was sind das für Lichter?", "Nachfragen", NodeAction.Goto("y4"))
        reply("Das Öl hast du schon. Entzünde das Feuer.", "Weiter", NodeAction.TurnIn("y2"))
        leave("Ich hole das Öl.")
    }
    node("y4", Npcs.YSA) {
        mood = "mystic"
        say("Laternen der Ertrunkenen. Sie locken Boote auf das Riff, wenn das Feuer schweigt. Brennt es, bleiben sie unten.")
        reply("Verstehe.", "Zurück", NodeAction.Goto("y1"))
    }
    node("y3", Npcs.YSA) {
        say("Drei Flaschen. Das genügt für viele Nächte.")
        reply("Entzünde das Feuer.", "Abschließen", NodeAction.TurnIn("y2"))
    }
    node("y2", Npcs.YSA) {
        mood = "mystic"
        say("Hörst du? Das Meer ist still. Das Leuchtfeuer brennt wieder.")
        leave("Endlich.")
    }
}
