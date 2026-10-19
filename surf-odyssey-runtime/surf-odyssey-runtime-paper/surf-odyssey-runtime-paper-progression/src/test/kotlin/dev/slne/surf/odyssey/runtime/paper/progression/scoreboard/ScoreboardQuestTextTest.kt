package dev.slne.surf.odyssey.runtime.paper.progression.scoreboard

import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks.StepSpec
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.Objective
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestProgress
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests for the quest block of the scoreboard. */
class ScoreboardQuestTextTest {
    private fun plain(component: Component) = PlainTextComponentSerializer.plainText().serialize(component)

    private val quest = TestPacks.questWithSteps(
        id = "fischer",
        steps = listOf(
            StepSpec(1, listOf(Objective.Harvest(Material.WHEAT, 12, null), Objective.Kill(EntityType.ZOMBIE, 2, null)))
        ),
    )

    @Test
    fun `name and objective with counts are separate`() {
        val result = ScoreboardQuestText.of(quest, QuestProgress(0, listOf(3, 0), false))

        assertEquals("fischer", plain(result!!.name))
        assertEquals("Schritt 0 · 3/12 · 0/2", plain(result.objective))
    }

    @Test
    fun `no quest gives no block`() {
        assertNull(ScoreboardQuestText.of(null, QuestProgress(0, listOf(0, 0), false)))
    }

    @Test
    fun `no progress gives no block`() {
        assertNull(ScoreboardQuestText.of(quest, null))
    }

    @Test
    fun `a step that does not exist gives no block`() {
        assertNull(ScoreboardQuestText.of(quest, QuestProgress(5, listOf(0, 0), false)))
    }
}
