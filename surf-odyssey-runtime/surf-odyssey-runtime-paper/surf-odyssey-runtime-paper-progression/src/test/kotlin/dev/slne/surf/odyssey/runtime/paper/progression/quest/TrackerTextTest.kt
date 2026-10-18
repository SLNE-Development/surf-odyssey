package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks.StepSpec
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.Objective
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.quest
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for the one-line text of the tracked quest. */
class TrackerTextTest {
    /** Returns the tracker text of [quest] with [progress] as plain text. */
    private fun plain(quest: QuestDefinition?, progress: QuestProgress?) =
        PlainTextComponentSerializer.plainText().serialize(TrackerText.render(quest, progress))

    @Test
    fun `the tracker shows title, step and counts on one line`() {
        val quest = TestPacks.questWithSteps(
            id = "fischer",
            steps = listOf(
                StepSpec(
                    1, listOf(Objective.Harvest(Material.WHEAT, 12, null), Objective.Kill(EntityType.ZOMBIE, 2, null))
                )
            ),
        )

        assertEquals("fischer · Schritt 0 · 3/12 · 0/2", plain(quest, QuestProgress(0, listOf(3, 0), false)))
    }

    @Test
    fun `line breaks in the step description become spaces`() {
        val quest = quest("netz", QuestType.SIDE, TestPacks.brina) {
            title = text("netz")
            offerNode = "netz_offer"
            step(1, TestPacks.brina, "netz_offer") {
                description = text("Fang\nFische")
                harvest(Material.WHEAT, 5)
            }
            node("netz_offer", TestPacks.brina) { text = text("Hallo") }
        }

        assertEquals("netz · Fang Fische · 1/5", plain(quest, QuestProgress(0, listOf(1), false)))
    }

    @Test
    fun `the tracker is empty without a tracked quest or progress`() {
        assertEquals("", plain(null, null))
        assertEquals("", plain(TestPacks.questWithVersion(1), null))
    }
}
