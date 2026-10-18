package dev.slne.surf.odyssey.runtime.paper.progression.definition

import dev.slne.surf.odyssey.runtime.paper.progression.definition.curve.Curve
import dev.slne.surf.odyssey.runtime.paper.progression.definition.curve.Curves
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.NodeAction
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.CustomItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.npc
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestBuilder
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.recipe
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentValidationException
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.station
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessorySlot
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolBonus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.tool
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.quest as questDefinition

/** Tests that each content validation rule reports its problem. */
class ValidationRulesTest {
    /** Valid pack every test adds one broken definition to. */
    private val base = TestPacks.valid()

    /** NPC of the valid pack. */
    private val brina = TestPacks.brina

    /** NPC that is not part of the pack. */
    private val stranger = TestPacks.npcNamed("stranger")

    /** Builds [pack] and returns the reported problems. */
    private fun problemsOf(pack: TestPacks.TestPack): List<String> =
        assertFailsWith<ContentValidationException> { ProgressionRegistry.build(pack) }.problems

    /** Returns the problems of the valid pack plus [quest]. */
    private fun problemsWithQuest(quest: QuestDefinition): List<String> =
        problemsOf(base.copy(quests = base.quests + quest))

    /** Returns the problems of the valid pack plus [tool]. */
    private fun problemsWithTool(tool: ToolDefinition): List<String> = problemsOf(base.copy(tools = listOf(tool)))

    /** Builds a side quest `q` given by [brina] with one talk step; [block] runs after the defaults. */
    private fun questQ(type: QuestType = QuestType.SIDE, block: QuestBuilder.() -> Unit = {}): QuestDefinition =
        questDefinition("q", type, brina) {
            title = text("Q")
            offerNode = "q_offer"
            step(1, brina, "q_offer") { talkTo(brina, "q_offer") }
            node("q_offer", brina) {
                text = text("Hallo")
                answer(text("Tschüss"), text("Beenden"), NodeAction.Close)
            }
            block()
        }

    @Test
    fun `a missing offer node is reported`() {
        val quest = questQ { offerNode = "nowhere" }
        assertTrue(problemsWithQuest(quest).any { "offer: missing node 'nowhere'" in it })
    }

    @Test
    fun `a missing entry node is reported`() {
        val quest = questQ { step(1, brina, "nowhere_entry") { talkTo(brina, "q_offer") } }
        assertTrue(problemsWithQuest(quest).any { "entry: missing node 'nowhere_entry'" in it })
    }

    @Test
    fun `an unknown node npc is reported`() {
        val quest = questQ { node("q_stranger", stranger) { text = text("Wer bist du?") } }
        assertTrue(problemsWithQuest(quest).any { "node 'q_stranger' npc references unknown npc 'stranger'" in it })
    }

    @Test
    fun `an unknown step npc is reported`() {
        val quest = questQ { step(1, stranger, "q_offer") { talkTo(brina, "q_offer") } }
        assertTrue(problemsWithQuest(quest).any { "step 1 npc references unknown npc 'stranger'" in it })
    }

    @Test
    fun `an unknown giver npc is reported`() {
        val quest = TestPacks.quest("q", giver = stranger)
        assertTrue(problemsWithQuest(quest).any { "giver references unknown npc 'stranger'" in it })
    }

    @Test
    fun `a recipe at a station outside the pack is reported`() {
        val otherBar = station("other_bar") {
            displayName = text("Andere Bar")
            icon = Material.SMOKER
        }
        val stray = recipe("stray", otherBar) { result(CustomItemRef(base.items.first()), 1) }
        assertTrue(problemsOf(base.copy(recipes = base.recipes + stray)).any { "unknown station 'other_bar'" in it })
    }

    @Test
    fun `a non-positive amount is reported`() {
        val bar = base.stations.first()
        val empty = recipe("empty", bar) {
            ingredient(CustomItemRef(base.items.first()), 0)
            result(CustomItemRef(base.items.last()), 1)
        }
        assertTrue(problemsOf(base.copy(recipes = base.recipes + empty)).any { "non-positive ingredient amount 0" in it })
    }

    @Test
    fun `a missing neutral portrait is reported`() {
        val faceless: NpcDefinition = npc("faceless") {
            name = text("Ohne Gesicht")
            skin("texture", "signature")
            greeting = text("...")
        }
        assertTrue(problemsOf(base.copy(npcs = base.npcs + faceless)).any { "no 'neutral' portrait" in it })
    }

    @Test
    fun `a recurring quest with two steps is reported`() {
        val quest = questQ(QuestType.DAILY) { step(2, brina, "q_offer") { talkTo(brina, "q_offer") } }
        assertTrue(problemsWithQuest(quest).any { "must have exactly one step" in it })
    }

    @Test
    fun `a curve that does not rise is reported`() {
        val flat = tool("flat_pick", ToolType.PICKAXE) {
            name = text("Flach")
            material = Material.IRON_PICKAXE
            curve = Curve { 0 }
        }
        assertTrue(problemsWithTool(flat).any { "not strictly increasing" in it })
    }

    @Test
    fun `a bonus above the maximum level is reported`() {
        val pick = tool("pick", ToolType.PICKAXE) {
            name = text("Hacke")
            material = Material.IRON_PICKAXE
            curve = Curves.PICKAXE
            bonus(11, ToolBonus.MaxDamage(10))
        }
        assertTrue(problemsWithTool(pick).any { "bonus at level 11" in it })
    }

    @Test
    fun `a slot below level one is reported`() {
        val pick = tool("pick", ToolType.PICKAXE) {
            name = text("Hacke")
            material = Material.IRON_PICKAXE
            curve = Curves.PICKAXE
            slot(0, AccessorySlot("bait", text("Köder")))
        }
        assertTrue(problemsWithTool(pick).any { "slot at level 0" in it })
    }
}
