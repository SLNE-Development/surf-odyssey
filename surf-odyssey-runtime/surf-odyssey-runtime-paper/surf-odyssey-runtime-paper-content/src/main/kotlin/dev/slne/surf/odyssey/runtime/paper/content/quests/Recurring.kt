package dev.slne.surf.odyssey.runtime.paper.content.quests

import dev.slne.surf.odyssey.runtime.paper.content.Items
import dev.slne.surf.odyssey.runtime.paper.content.Npcs
import dev.slne.surf.odyssey.runtime.paper.content.Recipes
import dev.slne.surf.odyssey.runtime.paper.content.Titles
import dev.slne.surf.odyssey.runtime.paper.content.Tools
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.AnswerKind
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.NodeAction
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestStepBuilder
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.quest
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.CurrencyReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.QuestReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.RecipeReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.TitleReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ToolReward
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import org.bukkit.entity.EntityType

/** The quests of the recurring pools: dailies from Olek, weeklies from Mira and monthlies from Ysa. */
object RecurringQuests {
    /** Daily: bring Olek 16 cod for 30 coins and 2 smoked fish. */
    val DAILY_COD: QuestDefinition = recurring(
        id = "daily_cod",
        type = QuestType.DAILY,
        giver = Npcs.OLEK,
        title = "Kabeljau für den Markt",
        step = "Bring Olek 16 Kabeljau",
        texts = Texts(
            offer = "Der Markt will heute 16 roher Kabeljau. Meine Knie wollen heute gar nichts. Hilfst du einem alten Mann?",
            accept = "Ich bringe dir den Kabeljau.",
            accepted = "Wunderbar. Ich setz mich solange auf die Kiste hier.",
            progress = "Hast du die 16 Kabeljau?",
            turnIn = "Hier, nimm.",
            delivered = "Sechzehn Stück, alle frisch. Der Markt wird zufrieden sein.",
            done = "Für deine Mühe. Und morgen wieder?",
        ),
        delivers = true,
        rewards = listOf(CurrencyReward("coins", 30.0), ItemReward(Items.SMOKED_FISH.ref, 2)),
    ) {
        deliver(Material.COD.ref, 16, Npcs.OLEK)
    }

    /** Daily: chop 32 spruce logs in `north_forest_spruce` for 30 coins and 2 copper sheets. */
    val DAILY_SPRUCE: QuestDefinition = recurring(
        id = "daily_spruce",
        type = QuestType.DAILY,
        giver = Npcs.OLEK,
        title = "Holz für den Steg",
        step = "Fälle 32 Fichtenstämme im Fichtenhain",
        texts = Texts(
            offer = "Der Steg fault mir unter den Füßen weg. Hol mir 32 Fichtenstämme aus dem Fichtenhain, dann flicke ich ihn.",
            accept = "Ich hole dir das Holz.",
            accepted = "Nimm eine scharfe Axt mit. Fichten sind stur.",
            progress = "Und, das Holz? 32 Stämme, sonst hält der Steg keinen Winter.",
            turnIn = "Das Holz liegt bereit.",
            done = "Riecht nach Harz und nach Arbeit. Danke dir.",
        ),
        rewards = listOf(CurrencyReward("coins", 30.0), ItemReward(Items.COPPER_SHEET.ref, 2)),
    ) {
        harvest(Material.SPRUCE_LOG, 32, "north_forest_spruce")
    }

    /** Daily: kill 3 wolves in `north_wolves` for 40 coins and 1 lamp oil. */
    val DAILY_WOLVES: QuestDefinition = recurring(
        id = "daily_wolves",
        type = QuestType.DAILY,
        giver = Npcs.OLEK,
        title = "Wölfe am Waldrand",
        step = "Erlege 3 Wölfe im Wolfsgebiet",
        texts = Texts(
            offer = "Die Wölfe kommen nachts bis an die Netze. Vertreib mir drei davon, bevor sie meine Fische holen.",
            accept = "Ich kümmere mich darum.",
            accepted = "Pass auf dich auf. Die beißen fester als Hechte.",
            progress = "Sind die Biester weg?",
            turnIn = "Drei Wölfe weniger.",
            done = "Dann schlafe ich heute ruhig. Hier, das hast du dir verdient.",
        ),
        rewards = listOf(CurrencyReward("coins", 40.0), ItemReward(Items.LANTERN_OIL.ref, 1)),
    ) {
        kill(EntityType.WOLF, 3, "north_wolves")
    }

    /** Weekly: mine 64 ores in `north_mine_ridge` for the fish platter recipe. */
    val WEEKLY_ORE: QuestDefinition = recurring(
        id = "weekly_ore",
        type = QuestType.WEEKLY,
        giver = Npcs.MIRA,
        title = "Erz für die Messinstrumente",
        step = "Baue 64 Erze am Frostgrat ab: 24 Eisenerz, 16 Tiefenschiefer-Eisenerz, 24 Steinkohle",
        texts = Texts(
            offer = "Meine Messinstrumente brauchen Metall, und der Frostgrat hat genug davon. Bring mir 64 Erze, dann verrate ich dir ein Rezept vom Ostkai.",
            accept = "Ich gehe zum Frostgrat.",
            accepted = "Der Grat ist kalt. Nimm etwas Warmes mit.",
            progress = "Wie läuft es am Grat?",
            turnIn = "Die Erze sind abgebaut.",
            done = "Gut. Hier, das Rezept für eine Fischplatte. Frag nicht, woher ich es habe.",
        ),
        mood = "thinking",
        rewards = listOf(RecipeReward(Recipes.FISH_PLATTER)),
    ) {
        harvest(Material.IRON_ORE, 24, "north_mine_ridge")
        harvest(Material.DEEPSLATE_IRON_ORE, 16, "north_mine_ridge")
        harvest(Material.COAL_ORE, 24, "north_mine_ridge")
    }

    /** Weekly: craft sushi 5 times for 120 coins. */
    val WEEKLY_COOK: QuestDefinition = recurring(
        id = "weekly_cook",
        type = QuestType.WEEKLY,
        giver = Npcs.MIRA,
        title = "Proviant für die Kartografin",
        step = "Bereite 5-mal Sushi an der Kochstelle zu",
        texts = Texts(
            offer = "Ich zeichne die ganze Nacht und vergesse zu essen. Bereite fünfmal Sushi zu, dann komme ich durch die Woche.",
            accept = "Ich koche für dich.",
            accepted = "Anselm hat eine Kochstelle in der Lobby.",
            progress = "Riecht nach Fisch. Ist das für mich?",
            turnIn = "Das Sushi ist fertig.",
            done = "Damit halte ich bis zur nächsten Flut durch. Danke.",
        ),
        rewards = listOf(CurrencyReward("coins", 120.0)),
    ) {
        craft(Recipes.SUSHI, 5)
    }

    /** Monthly: catch 200 fish in `north_lake`, 140 cod and 60 salmon, for the title "Angler vom Ostkai". */
    val MONTHLY_ANGLER: QuestDefinition = recurring(
        id = "monthly_angler",
        type = QuestType.MONTHLY,
        giver = Npcs.YSA,
        title = "Der Fang des Mondes",
        step = "Fange 200 Fische im Eissee: 140 Kabeljau, 60 Lachse",
        texts = Texts(
            offer = "Wenn der Mond voll ist, steigt das Wasser über die Mole. Dann will das Meer wissen, wer es kennt. Fang 200 Fische im Eissee, und es gibt dir einen Namen.",
            accept = "Ich werfe die Angel aus.",
            accepted = "Das Meer zählt mit.",
            progress = "Das Meer hat deine Fänge gezählt. Ist es genug?",
            turnIn = "Zweihundert Fische.",
            done = "Von heute an kennt man dich als Angler vom Ostkai.",
        ),
        mood = "mystic",
        rewards = listOf(TitleReward(Titles.ANGLER)),
    ) {
        fish(140, material = Material.COD, regionId = "north_lake")
        fish(60, material = Material.SALMON, regionId = "north_lake")
    }

    /** Monthly: make 10 fish platters for the pit pickaxe. */
    val MONTHLY_FEAST: QuestDefinition = recurring(
        id = "monthly_feast",
        type = QuestType.MONTHLY,
        giver = Npcs.YSA,
        title = "Ein Festmahl für die Flut",
        step = "Stelle 10 Fischplatten her",
        texts = Texts(
            offer = "Wenn der Mond voll ist, steigt das Wasser über die Mole. Die Alten deckten dann eine Tafel für das Meer. Bereite zehn Fischplatten, und wir tun es ihnen gleich.",
            accept = "Ich decke die Tafel.",
            accepted = "Zehn Platten. Das Meer ist hungrig.",
            progress = "Ist die Tafel gedeckt?",
            turnIn = "Zehn Fischplatten, fertig.",
            done = "Das Meer ist satt und still. Nimm diese Hacke, sie hat lange in der Tiefe gelegen.",
        ),
        mood = "mystic",
        rewards = listOf(ToolReward(Tools.PIT_PICKAXE)),
    ) {
        craft(Recipes.FISH_PLATTER, 10)
    }

    /** All recurring quests in the order they are registered. */
    val all: List<QuestDefinition> = listOf(
        DAILY_COD, DAILY_SPRUCE, DAILY_WOLVES, WEEKLY_ORE, WEEKLY_COOK, MONTHLY_ANGLER, MONTHLY_FEAST,
    )
}

/**
 * The German lines of a recurring quest.
 *
 * @property offer what the giver says when offering the quest
 * @property accept the answer that accepts the quest
 * @property accepted what the giver says after the quest was accepted
 * @property progress what the giver says while the quest is open
 * @property turnIn the answer that hands in the items or turns the quest in
 * @property delivered what the giver says after the items were handed in; only used by delivery quests
 * @property done what the giver says after the quest was completed
 */
private class Texts(
    val offer: String,
    val accept: String,
    val accepted: String,
    val progress: String,
    val turnIn: String,
    val delivered: String? = null,
    val done: String,
)

/**
 * Defines a recurring quest with one step at its giver and the nodes `<id>_offer`, `<id>_accepted`, `<id>_progress`,
 * `<id>_done` and, for delivery quests, `<id>_delivered`.
 *
 * In a delivery quest the progress node hands the items in and leads to the delivered node, which turns the quest
 * in; it also offers to turn in directly once the items were handed in. Other quests are turned in from the progress
 * node.
 *
 * @param id the quest ID
 * @param type the recurring quest type
 * @param giver the NPC that gives the quest and takes it back
 * @param title the German quest title
 * @param step the German description of the only step
 * @param texts the German lines of the nodes
 * @param delivers whether the step hands items to the giver
 * @param mood the portrait mood of the giver in the offer and done nodes
 * @param rewards the quest rewards
 * @param objectives adds the objectives of the step
 */
private fun recurring(
    id: String,
    type: QuestType,
    giver: NpcDefinition,
    title: String,
    step: String,
    texts: Texts,
    delivers: Boolean = false,
    mood: String = NpcDefinition.NEUTRAL_MOOD,
    rewards: List<QuestReward>,
    objectives: QuestStepBuilder.() -> Unit,
): QuestDefinition = quest(id, type, giver) {
    this.title = text(title)
    description = text(step)
    offerNode = "${id}_offer"

    step(1, giver, "${id}_progress") {
        description = text(step)
        objectives()
    }
    rewards.forEach(::reward)

    node("${id}_offer", giver) {
        this.mood = mood
        say(texts.offer)
        reply(texts.accept, "Quest annehmen", NodeAction.AcceptQuest("${id}_accepted"), AnswerKind.ACCEPT)
        leave("Ein andermal.")
    }
    node("${id}_accepted", giver) {
        say(texts.accepted)
        leave("Bis später.")
    }
    node("${id}_progress", giver) {
        say(texts.progress)
        if (delivers) {
            reply(texts.turnIn, "Abgeben", NodeAction.DeliverItems("${id}_delivered"))
            reply("Das habe ich dir schon gebracht.", "Abschließen", NodeAction.TurnIn("${id}_done"))
        } else {
            reply(texts.turnIn, "Abschließen", NodeAction.TurnIn("${id}_done"))
        }
        leave("Ich bin noch dabei.")
    }
    if (delivers) {
        node("${id}_delivered", giver) {
            say(checkNotNull(texts.delivered) { "delivery quest $id needs a delivered text" })
            reply("Gern geschehen.", "Abschließen", NodeAction.TurnIn("${id}_done"))
        }
    }
    node("${id}_done", giver) {
        this.mood = mood
        say(texts.done)
        leave("Bis zum nächsten Mal.")
    }
}
