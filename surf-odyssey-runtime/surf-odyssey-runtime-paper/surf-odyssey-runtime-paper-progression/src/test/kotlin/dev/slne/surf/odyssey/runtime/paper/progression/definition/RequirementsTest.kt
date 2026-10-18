package dev.slne.surf.odyssey.runtime.paper.progression.definition

import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestStateView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirements
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for the requirement combinators. */
class RequirementsTest {
    @Test
    fun `step reached counts completed quests`() {
        val ctx = FakeContext(quests = mapOf("q" to QuestStateView(QuestStatus.COMPLETED, 0, 0)))
        assertTrue(Requirements.questStepReached(TestPacks.quest("q"), 3).isMet(ctx))
    }

    @Test
    fun `abandoned quests are not completed`() {
        val ctx = FakeContext(quests = mapOf("q" to QuestStateView(QuestStatus.ABANDONED, 0, 0)))
        assertFalse(Requirements.questCompleted(TestPacks.quest("q")).isMet(ctx))
    }
}
