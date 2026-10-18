package dev.slne.surf.odyssey.runtime.paper.progression.definition

import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.AnswerKind
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.NodeAction
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.CustomItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.itemDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.npc
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.recipe
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentPack
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.RecurringPools
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.station
import dev.slne.surf.odyssey.runtime.paper.progression.definition.title.TitleDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessoryDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.quest as questDefinition

/**
 * Small content packs and definitions for tests.
 *
 * All quests are given by the harbour master NPC [brina]. Every call builds fresh definition objects.
 */
object TestPacks {
    /** Harbour master NPC with a neutral portrait and no trades. */
    val brina: NpcDefinition = npc("brina") {
        name = text("Brina")
        skin("texture", "signature")
        portrait("neutral", Key.key("odyssey", "portrait_brina_neutral"))
        greeting = text("Moin!")
    }

    /**
     * Builds a one-step quest given by [brina] whose only node is its offer node `<id>_offer`.
     *
     * @param id the quest ID
     * @param type the quest type
     */
    fun quest(id: String, type: QuestType = QuestType.SIDE): QuestDefinition = questDefinition(id, type, brina) {
        title = text(id)
        description = text("Quest $id")
        offerNode = "${id}_offer"
        step(1, brina, "${id}_offer") {
            talkTo(brina, "${id}_offer")
        }
        node("${id}_offer", brina) {
            text = text("Hilfst du mir?")
            answer(text("Ja"), text("Quest annehmen"), NodeAction.AcceptQuest(null), AnswerKind.ACCEPT)
            answer(text("Nein"), text("Gespräch beenden"), NodeAction.Close, AnswerKind.LEAVE)
        }
    }

    /**
     * Builds a pack that passes validation: rice, fish and sushi items, a sushi station with one recipe, one main
     * quest, three daily, one weekly and one monthly quest.
     *
     * @param dailyCount how many daily quests are drawn per period
     */
    fun valid(dailyCount: Int = 2): ContentPack = pack(RecurringPools(dailyCount = dailyCount))

    /**
     * Builds the [valid] pack plus a second item called `sushi` and a quest whose answer jumps to the missing node
     * `brina_nope`.
     */
    fun broken(): ContentPack {
        val base = pack(RecurringPools())
        val brokenQuest = questDefinition("lost_net", QuestType.SIDE, brina) {
            title = text("Das verlorene Netz")
            description = text("Brina sucht ihr Netz.")
            offerNode = "lost_net_offer"
            step(1, brina, "lost_net_offer") {
                talkTo(brina, "lost_net_offer")
            }
            node("lost_net_offer", brina) {
                text = text("Hast du mein Netz gesehen?")
                answer(text("Wo?"), text("Nachfragen"), NodeAction.Goto("brina_nope"))
            }
        }
        return TestPack(
            items = base.items + item("sushi", Material.COOKED_SALMON),
            stations = base.stations,
            recipes = base.recipes,
            npcs = base.npcs,
            quests = base.quests + brokenQuest,
            pools = base.pools,
        )
    }

    /** Builds an item definition whose display name is its ID. */
    private fun item(id: String, material: Material): ItemDefinition = itemDefinition(id) {
        displayName = text(id)
        this.material = material
    }

    /** Builds the content of [valid] with the given pools. */
    private fun pack(pools: RecurringPools): ContentPack {
        val rice = item("rice", Material.WHEAT)
        val fish = item("fish", Material.COD)
        val sushi = item("sushi", Material.COOKED_COD)
        val bar = station("sushi_bar") {
            displayName = text("Sushi-Bar")
            icon = Material.SMOKER
        }
        val sushiRecipe = recipe("sushi", bar) {
            ingredient(CustomItemRef(rice), 2)
            ingredient(CustomItemRef(fish), 1)
            result(CustomItemRef(sushi), 1)
            unlockedByDefault = true
        }
        val quests = listOf(
            quest("harbor_intro", QuestType.MAIN),
            quest("d1", QuestType.DAILY),
            quest("d2", QuestType.DAILY),
            quest("d3", QuestType.DAILY),
            quest("w1", QuestType.WEEKLY),
            quest("m1", QuestType.MONTHLY),
        )
        return TestPack(
            items = listOf(rice, fish, sushi),
            stations = listOf(bar),
            recipes = listOf(sushiRecipe),
            npcs = listOf(brina),
            quests = quests,
            pools = pools,
        )
    }

    /** Plain [ContentPack] holding the given lists. */
    data class TestPack(
        override val items: List<ItemDefinition> = emptyList(),
        override val stations: List<StationDefinition> = emptyList(),
        override val recipes: List<RecipeDefinition> = emptyList(),
        override val titles: List<TitleDefinition> = emptyList(),
        override val npcs: List<NpcDefinition> = emptyList(),
        override val quests: List<QuestDefinition> = emptyList(),
        override val tools: List<ToolDefinition> = emptyList(),
        override val accessories: List<AccessoryDefinition> = emptyList(),
        override val pools: RecurringPools = RecurringPools(),
    ) : ContentPack
}
