package dev.slne.surf.odyssey.runtime.microservice.progression

import dev.slne.surf.odyssey.core.common.progression.OperationDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.core.common.progression.StateChange
import java.util.*
import kotlin.test.Test
import kotlin.test.assertIs

/** Tests for the pure decision logic that applies progression operations. */
class OperationApplierTest {
    private val player = UUID.randomUUID()
    private fun state(vararg quests: QuestStateDto) = ProgressionStateDto(player, quests.toList(), emptyList(),
        emptySet(), emptySet(), null, null, emptyList(), emptyList())
    private fun op(vararg changes: StateChange) = OperationDto(UUID.randomUUID(), player, changes.toList(), emptyList())

    @Test fun `accepting an active quest of the same period is rejected`() {
        val s = state(QuestStateDto("q", QuestStatus.ACTIVE, 0, 1, 5, null))
        assertIs<Decision.Reject>(OperationApplier.decide(s, op(StateChange.QuestAccepted("q", 1, 5))))
    }
    @Test fun `accepting a recurring quest of a new period replaces it`() {
        val s = state(QuestStateDto("q", QuestStatus.COMPLETED, 0, 1, 5, 1L))
        assertIs<Decision.Apply>(OperationApplier.decide(s, op(StateChange.QuestAccepted("q", 1, 6))))
    }
    @Test fun `completing from a stale step is rejected`() {
        val s = state(QuestStateDto("q", QuestStatus.ACTIVE, 2, 1, 0, null))
        assertIs<Decision.Reject>(OperationApplier.decide(s, op(StateChange.QuestCompleted("q", 1))))
    }
    @Test fun `one rejected change rejects the whole operation`() {
        val s = state()
        assertIs<Decision.Reject>(OperationApplier.decide(s,
            op(StateChange.RecipeUnlocked("sushi"), StateChange.JobCollected(UUID.randomUUID()))))
    }
    @Test fun `unlocking a known recipe is applied without error`() {
        val s = state().copy(recipes = setOf("sushi"))
        assertIs<Decision.Apply>(OperationApplier.decide(s, op(StateChange.RecipeUnlocked("sushi"))))
    }
}
