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
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.Objective
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirements
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.QuestReward
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import kotlin.time.Duration
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
     * @param giver the NPC giving the quest and speaking its node
     * @param requirement requirement to be offered the quest
     */
    fun quest(
        id: String,
        type: QuestType = QuestType.SIDE,
        giver: NpcDefinition = brina,
        requirement: Requirement = Requirements.ALWAYS,
    ): QuestDefinition =
        questDefinition(id, type, giver) {
            title = text(id)
            description = text("Quest $id")
            offerNode = "${id}_offer"
            this.requirement = requirement
            step(1, giver, "${id}_offer") {
                talkTo(giver, "${id}_offer")
            }
            node("${id}_offer", giver) {
                text = text("Hilfst du mir?")
                answer(text("Ja"), text("Quest annehmen"), NodeAction.AcceptQuest(null), AnswerKind.ACCEPT)
                answer(text("Nein"), text("Gespräch beenden"), NodeAction.Close, AnswerKind.LEAVE)
            }
        }

    /**
     * One step of a quest built by [questWithSteps].
     *
     * @property version the step version
     * @property objectives the objectives of the step
     * @property rewards the rewards of the step
     */
    data class StepSpec(
        val version: Int,
        val objectives: List<Objective>,
        val rewards: List<QuestReward> = emptyList(),
    )

    /**
     * Builds a quest given by [brina] with the given steps. Step `i` is described as `Schritt i` and is turned in at
     * the offer node `<id>_offer`.
     *
     * @param id the quest ID
     * @param type the quest type
     * @param steps the steps in order
     * @param rewards the rewards of the quest
     */
    fun questWithSteps(
        id: String = "versioned",
        type: QuestType = QuestType.SIDE,
        steps: List<StepSpec>,
        rewards: List<QuestReward> = emptyList(),
    ): QuestDefinition = questDefinition(id, type, brina) {
        title = text(id)
        description = text("Quest $id")
        offerNode = "${id}_offer"
        steps.forEachIndexed { index, spec ->
            step(spec.version, brina, "${id}_offer") {
                description = text("Schritt $index")
                spec.objectives.forEach(::objective)
                spec.rewards.forEach(::reward)
            }
        }
        rewards.forEach(::reward)
        node("${id}_offer", brina) {
            text = text("Hilfst du mir?")
            answer(text("Ja"), text("Quest annehmen"), NodeAction.AcceptQuest(null), AnswerKind.ACCEPT)
        }
    }

    /**
     * Builds a one-step quest whose step has [stepVersion] and one objective to harvest [amount] wheat.
     *
     * @param stepVersion the version of the only step
     * @param amount how much wheat the objective needs
     * @param type the quest type
     */
    fun questWithVersion(stepVersion: Int, amount: Int = 12, type: QuestType = QuestType.SIDE): QuestDefinition =
        questWithSteps(
            type = type,
            steps = listOf(StepSpec(stepVersion, listOf(Objective.Harvest(Material.WHEAT, amount, null)))),
        )

    /**
     * Builds a pack that passes validation: rice, fish and sushi items, a sushi station with one recipe, one main
     * quest, three daily, one weekly and one monthly quest.
     *
     * @param dailyCount how many daily quests are drawn per period
     */
    fun valid(dailyCount: Int = 2): TestPack = pack(RecurringPools(dailyCount = dailyCount))

    /**
     * Builds the [valid] pack plus a second item called `sushi` and a quest whose answer jumps to the missing node
     * `brina_nope`.
     */
    fun broken(): TestPack {
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

    /**
     * Builds an NPC with a neutral portrait.
     *
     * @param id the NPC ID
     * @param visibleWhen requirement to see the NPC
     */
    fun npcNamed(id: String, visibleWhen: Requirement? = null): NpcDefinition = npc(id) {
        name = text(id)
        skin("texture", "signature")
        portrait("neutral", Key.key("odyssey", "portrait_${id}_neutral"))
        greeting = text("Hallo!")
        this.visibleWhen = visibleWhen
    }

    /**
     * Builds a `sushi` recipe at a `sushi_bar` station that consumes rice and fish items and makes one sushi item.
     * The items `rice`, `fish` and `sushi` are named by their IDs.
     *
     * @param rice how much rice one craft consumes
     * @param fish how much fish one craft consumes
     * @param duration how long one craft takes
     * @param unlockedByDefault whether every player knows the recipe
     */
    fun sushiRecipe(
        rice: Int,
        fish: Int,
        duration: Duration = Duration.ZERO,
        unlockedByDefault: Boolean = false,
    ): RecipeDefinition {
        val bar = station("sushi_bar") {
            displayName = text("Sushi-Bar")
            icon = Material.SMOKER
        }
        return recipe("sushi", bar) {
            ingredient(CustomItemRef(item("rice", Material.WHEAT)), rice)
            ingredient(CustomItemRef(item("fish", Material.COD)), fish)
            result(CustomItemRef(item("sushi", Material.COOKED_COD)), 1)
            this.duration = duration
            this.unlockedByDefault = unlockedByDefault
        }
    }

    /** Builds an item definition whose display name is its ID. */
    fun item(id: String, material: Material): ItemDefinition = itemDefinition(id) {
        displayName = text(id)
        this.material = material
    }

    /** Builds the content of [valid] with the given pools. */
    private fun pack(pools: RecurringPools): TestPack {
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
