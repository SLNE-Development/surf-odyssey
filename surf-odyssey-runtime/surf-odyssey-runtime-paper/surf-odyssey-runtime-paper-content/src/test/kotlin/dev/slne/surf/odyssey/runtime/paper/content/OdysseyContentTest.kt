package dev.slne.surf.odyssey.runtime.paper.content

import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.papermc.paper.registry.RegistryAccess
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component.text
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.NodeAction
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.Objective
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestStepRequirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.RecipeUnlockedRequirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.CurrencyReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.RecipeReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.TitleReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ToolReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.Price
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeResult
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.entity.EntityType
import org.bukkit.attribute.Attribute
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Tests that the content of the Odyssey servers forms a valid progression registry. */
class OdysseyContentTest {
    /** The registry built from the content. */
    private val registry = ProgressionRegistry.build(OdysseyContent)

    /**
     * Installs a stand-in registry access whose registries return mocks, so that registry values such as attributes
     * load without a server.
     */
    companion object {
        init {
            mockkStatic(RegistryAccess::class)
            val access = proxy(RegistryAccess::class.java) { _, _ ->
                proxy(Registry::class.java) { method, _ -> if (method.name == "getOrThrow") mockk<Attribute>() else null }
            }
            every { RegistryAccess.registryAccess() } returns access
        }

        /**
         * Creates a JDK proxy that implements the interface [type] and answers every call with [answer], which
         * receives the called method and its arguments. Unlike a mock, creating it does not redefine the interface.
         */
        private fun <T> proxy(type: Class<T>, answer: (Method, Array<out Any?>?) -> Any?): T =
            type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args -> answer(method, args) })
    }

    @Test
    fun `registry builds from the content`() {
        assertNotNull(registry)
    }

    @Test
    fun `every custom item uses the craft engine item of the same id`() {
        registry.allItems.forEach { item ->
            assertEquals(Key.key("odyssey", item.id), item.craftEngineId, "item ${item.id}")
        }
    }

    @Test
    fun `the content defines the items stations recipes titles tools and accessories`() {
        assertEquals(
            listOf(
                "odyssey_book", "sushi", "smoked_fish", "fish_soup", "copper_sheet", "lantern_oil", "bait_worm",
                "drill_bit", "fish_platter",
            ),
            registry.allItems.map { it.id },
        )
        assertEquals(listOf("workbench", "cooking_station"), registry.allStations.map { it.id })
        assertEquals(
            listOf(
                "copper_sheet", "lantern_oil", "bait_worm", "drill_bit", "sushi", "smoked_fish", "fish_soup",
                "fish_platter",
            ),
            registry.allRecipes.map { it.id },
        )
        assertEquals(listOf("lightkeeper", "angler"), registry.allTitles.map { it.id })
        assertEquals(listOf("harbor_rod", "pit_pickaxe"), registry.allTools.map { it.id })
        assertEquals(listOf("bait_worm", "drill_bit"), registry.allAccessories.map { it.id })
    }

    @Test
    fun `the content defines the npcs and the one-time quests`() {
        assertEquals(listOf("brina", "koch", "olek", "mira", "ysa"), registry.allNpcs.map { it.id })
        assertEquals(QuestType.MAIN, registry.quest("leuchtfeuer")?.type)
        assertEquals(QuestType.SIDE, registry.quest("koch_first_dish")?.type)
    }

    @Test
    fun `the lighthouse questline leads from brina over olek and mira to ysa`() {
        val quest = assertNotNull(registry.quest("leuchtfeuer"))
        assertEquals("brina", quest.giver.id)
        assertEquals(listOf("olek", "olek", "mira", "ysa"), quest.steps.map { it.npc.id })
        assertEquals(Objective.TalkTo(Npcs.OLEK, quest.steps[0].entryNode), quest.steps[0].objectives.single())
        assertEquals(Objective.Fish(null, Material.SALMON, 5, "north_lake"), quest.steps[1].objectives.single())
        assertEquals(listOf(RecipeReward(Recipes.BAIT_WORM)), quest.steps[1].rewards)
        assertEquals(text("Fange 5 Lachse im Eissee (nachts beißen sie)"), quest.steps[1].description)
        assertEquals(Objective.Deliver(Material.INK_SAC.ref, 12, Npcs.MIRA), quest.steps[2].objectives.single())
        assertEquals(Objective.Deliver(Items.LANTERN_OIL.ref, 3, Npcs.YSA), quest.steps[3].objectives.single())
        assertEquals(
            listOf(
                TitleReward(Titles.LIGHTKEEPER),
                RecipeReward(Recipes.DRILL_BIT),
                ToolReward(Tools.HARBOR_ROD),
                CurrencyReward("coins", 200.0),
            ),
            quest.rewards,
        )
    }

    @Test
    fun `ysa is visible from the third chapter of the lighthouse questline`() {
        assertEquals(QuestStepRequirement("leuchtfeuer", 2), Npcs.YSA.visibleWhen)
    }

    @Test
    fun `the first dish of the cook asks for a fish soup and gives sushi`() {
        val quest = assertNotNull(registry.quest("koch_first_dish"))
        assertEquals("koch", quest.giver.id)
        assertEquals(Objective.Craft(Recipes.FISH_SOUP, 1), quest.steps.single().objectives.single())
        assertEquals(listOf(ItemReward(Items.SUSHI.ref, 3)), quest.rewards)
    }

    @Test
    fun `the cook trades recipes and bait and runs the cooking station`() {
        val koch = Npcs.KOCH
        assertEquals(Stations.COOKING_STATION, koch.station)
        val results = koch.trades.map { it.result }
        assertTrue(TradeResult.Recipe(Recipes.SUSHI) in results)
        assertTrue(TradeResult.Recipe(Recipes.SMOKED_FISH) in results)
        assertTrue(TradeResult.Item(Items.BAIT_WORM.ref, 4) in results)
        assertEquals(1, koch.trades.count { it.price is Price.Currency })
    }

    @Test
    fun `every node of every quest is reachable from its offer or a step entry`() {
        registry.allQuests.forEach { quest ->
            val reachable = reachableNodes(quest, listOf(quest.offerNode) + quest.steps.map { it.entryNode }, ::targetOf)
            assertEquals(quest.nodes.map { it.id }.toSet(), reachable, "quest ${quest.id}")
        }
    }

    @Test
    fun `every quest step can be turned in from its entry node`() {
        registry.allQuests.forEach { quest ->
            quest.steps.forEachIndexed { index, step ->
                val nodes = reachableNodes(quest, listOf(step.entryNode), ::sameStepTargetOf)
                val turnIn = nodes.map { registry.node(quest, it) }.any { node ->
                    node.npc.id == step.npc.id && node.answers.any { it.action is NodeAction.TurnIn }
                }
                assertTrue(turnIn, "quest ${quest.id} step $index")
            }
        }
    }

    @Test
    fun `the weekly ore quest waits for the third chapter and the feast for the platter recipe`() {
        assertEquals(QuestStepRequirement("leuchtfeuer", 2), registry.quest("weekly_ore")?.requirement)
        assertEquals(RecipeUnlockedRequirement("fish_platter"), registry.quest("monthly_feast")?.requirement)
    }

    @Test
    fun `olek takes the salmon step as caught instead of handed over`() {
        val quest = assertNotNull(registry.quest("leuchtfeuer"))
        val turnIn = registry.node(quest, quest.steps[1].entryNode).answers.single { it.action is NodeAction.TurnIn }
        assertEquals(text("Fünf Lachse, gefangen."), turnIn.text)
    }

    @Test
    fun `every node mood has a portrait of its npc`() {
        registry.allQuests.forEach { quest ->
            quest.nodes.forEach { node ->
                assertTrue(node.mood in node.npc.portraits, "quest ${quest.id} node ${node.id} mood ${node.mood}")
            }
        }
    }

    @Test
    fun `every npc portrait is an image of the craft engine pack`() {
        val images = portraitImages()
        registry.allNpcs.forEach { npc ->
            npc.portraits.forEach { (mood, key) ->
                assertTrue(key.asString() in images, "npc ${npc.id} mood $mood portrait $key")
            }
        }
    }

    @Test
    fun `the recurring pools draw two dailies one weekly and one monthly`() {
        assertEquals(listOf("daily_cod", "daily_spruce", "daily_wolves"), registry.pool(QuestType.DAILY).map { it.id })
        assertEquals(listOf("weekly_ore", "weekly_cook"), registry.pool(QuestType.WEEKLY).map { it.id })
        assertEquals(listOf("monthly_angler", "monthly_feast"), registry.pool(QuestType.MONTHLY).map { it.id })
        assertEquals(2, registry.pools.dailyCount)
        assertEquals(1, registry.pools.weeklyCount)
        assertEquals(1, registry.pools.monthlyCount)
    }

    @Test
    fun `dailies come from olek weeklies from mira and monthlies from ysa`() {
        assertTrue(registry.pool(QuestType.DAILY).all { it.giver == Npcs.OLEK })
        assertTrue(registry.pool(QuestType.WEEKLY).all { it.giver == Npcs.MIRA })
        assertTrue(registry.pool(QuestType.MONTHLY).all { it.giver == Npcs.YSA })
    }

    @Test
    fun `the recurring quests have the objectives and rewards of the pools`() {
        assertObjectives("daily_cod", Objective.Deliver(Material.COD.ref, 16, Npcs.OLEK))
        assertObjectives("daily_spruce", Objective.Harvest(Material.SPRUCE_LOG, 32, "north_forest_spruce"))
        assertObjectives("daily_wolves", Objective.Kill(EntityType.WOLF, 3, "north_wolves"))
        assertObjectives(
            "weekly_ore",
            Objective.Harvest(Material.IRON_ORE, 24, "north_mine_ridge"),
            Objective.Harvest(Material.DEEPSLATE_IRON_ORE, 16, "north_mine_ridge"),
            Objective.Harvest(Material.COAL_ORE, 24, "north_mine_ridge"),
        )
        assertObjectives("weekly_cook", Objective.Craft(Recipes.SUSHI, 5))
        assertObjectives(
            "monthly_angler",
            Objective.Fish(null, Material.COD, 140, "north_lake"),
            Objective.Fish(null, Material.SALMON, 60, "north_lake"),
        )
        assertObjectives("monthly_feast", Objective.Craft(Recipes.FISH_PLATTER, 10))
        assertEquals(64, registry.quest("weekly_ore")!!.steps.single().objectives.sumOf { it.amount })
        assertEquals(200, registry.quest("monthly_angler")!!.steps.single().objectives.sumOf { it.amount })

        assertEquals(
            listOf(CurrencyReward("coins", 30.0), ItemReward(Items.SMOKED_FISH.ref, 2)),
            registry.quest("daily_cod")?.rewards,
        )
        assertEquals(
            listOf(CurrencyReward("coins", 30.0), ItemReward(Items.COPPER_SHEET.ref, 2)),
            registry.quest("daily_spruce")?.rewards,
        )
        assertEquals(
            listOf(CurrencyReward("coins", 40.0), ItemReward(Items.LANTERN_OIL.ref, 1)),
            registry.quest("daily_wolves")?.rewards,
        )
        assertEquals(listOf(RecipeReward(Recipes.FISH_PLATTER)), registry.quest("weekly_ore")?.rewards)
        assertEquals(listOf(CurrencyReward("coins", 120.0)), registry.quest("weekly_cook")?.rewards)
        assertEquals(listOf(TitleReward(Titles.ANGLER)), registry.quest("monthly_angler")?.rewards)
        assertEquals(listOf(ToolReward(Tools.PIT_PICKAXE)), registry.quest("monthly_feast")?.rewards)
    }

    /** Asserts that the only step of the quest [id] has exactly [objectives]. */
    private fun assertObjectives(id: String, vararg objectives: Objective) {
        assertEquals(objectives.toList(), registry.quest(id)?.steps?.single()?.objectives, "quest $id")
    }

    /** Returns the node an answer with [action] leads to, or `null` if it leaves the quest's conversation. */
    private fun targetOf(action: NodeAction): String? = when (action) {
        is NodeAction.Goto -> action.nodeId
        is NodeAction.AcceptQuest -> action.then
        is NodeAction.CompleteTalk -> action.then
        is NodeAction.DeliverItems -> action.then
        is NodeAction.TurnIn -> action.then
        NodeAction.OpenTrade, NodeAction.OpenStation, NodeAction.Close -> null
    }

    /**
     * Returns the node an answer with [action] leads to while the quest stays at the same step, or `null` if the
     * answer leaves the conversation or moves the quest on.
     */
    private fun sameStepTargetOf(action: NodeAction): String? = when (action) {
        is NodeAction.Goto -> action.nodeId
        is NodeAction.CompleteTalk -> action.then
        is NodeAction.DeliverItems -> action.then
        is NodeAction.AcceptQuest, is NodeAction.TurnIn, NodeAction.OpenTrade, NodeAction.OpenStation, NodeAction.Close -> null
    }

    /**
     * Returns the IDs of the nodes of [quest] reachable from [roots] by following the answers whose target [next]
     * returns, including the roots.
     */
    private fun reachableNodes(quest: QuestDefinition, roots: List<String>, next: (NodeAction) -> String?): Set<String> {
        val reachable = mutableSetOf<String>()
        val open = ArrayDeque(roots)
        while (open.isNotEmpty()) {
            val id = open.removeFirst()
            if (!reachable.add(id)) continue
            registry.node(quest, id).answers.mapNotNullTo(open) { next(it.action) }
        }
        return reachable
    }

    /** Reads the image IDs defined in the CraftEngine portrait configuration of the repository. */
    private fun portraitImages(): Set<String> {
        var dir: Path? = Path.of("").toAbsolutePath()
        while (dir != null && !dir.resolve("settings.gradle.kts").exists()) dir = dir.parent
        val root = assertNotNull(dir, "repository root not found")
        val file = root.resolve("docker/paper/plugins/CraftEngine/resources/odyssey/configuration/images/portraits.yml")
        return Regex("""^ {2}([a-z0-9_.-]+:[a-z0-9_./-]+):\s*$""", RegexOption.MULTILINE)
            .findAll(file.readText())
            .map { it.groupValues[1] }
            .toSet()
    }
}
