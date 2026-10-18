package dev.slne.surf.odyssey.runtime.microservice.progression

import dev.slne.surf.odyssey.core.common.progression.OperationDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.core.common.progression.NewDeliveryDto
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.core.common.progression.StationJobDto
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Tests for the pure decision logic that applies progression operations. */
class OperationApplierTest {
    private val player = UUID.randomUUID()
    /** Returns the stored progression of [player] with only [quests]. */
    private fun state(vararg quests: QuestStateDto) = ProgressionStateDto(player, quests.toList(), emptyList(),
        emptySet(), emptySet(), null, null, emptyList(), emptyList())
    /** Returns an operation of [player] with a new id that makes [changes] and delivers nothing. */
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
    @Test fun `re-accepting an abandoned quest of the same period is applied`() {
        val s = state(QuestStateDto("q", QuestStatus.ABANDONED, 1, 1, 5, null))
        assertIs<Decision.Apply>(OperationApplier.decide(s, op(StateChange.QuestAccepted("q", 1, 5))))
    }
    @Test fun `advancing a quest that is not active is rejected`() {
        val s = state(QuestStateDto("q", QuestStatus.COMPLETED, 0, 1, 0, 1L))
        assertIs<Decision.Reject>(OperationApplier.decide(s, op(StateChange.QuestStepAdvanced("q", 0, 1, 1))))
    }
    @Test fun `advancing a quest from the wrong step is rejected`() {
        val s = state(QuestStateDto("q", QuestStatus.ACTIVE, 1, 1, 0, null))
        assertIs<Decision.Reject>(OperationApplier.decide(s, op(StateChange.QuestStepAdvanced("q", 0, 1, 1))))
    }
    @Test fun `accepting and advancing a quest in one operation is applied`() {
        assertIs<Decision.Apply>(OperationApplier.decide(state(),
            op(StateChange.QuestAccepted("q", 1, 0), StateChange.QuestStepAdvanced("q", 0, 1, 1))))
    }
    @Test fun `starting a job with an id that is already stored is rejected`() {
        val job = StationJobDto(UUID.randomUUID(), "oven", "sushi", 1, 0, 1_000)
        assertIs<Decision.Reject>(OperationApplier.decide(state(), op(StateChange.JobStarted(job)), setOf(job.id)))
    }
    @Test fun `an id longer than its column is rejected`() {
        val id = "a".repeat(ProgressionLimits.MAX_ID_LENGTH + 1)
        assertIs<Decision.Reject>(OperationApplier.decide(state(), op(StateChange.RecipeUnlocked(id))))
    }
    @Test fun `a delivery source longer than its column is rejected`() {
        val operation = OperationDto(UUID.randomUUID(), player, emptyList(),
            listOf(NewDeliveryDto("item", "s".repeat(ProgressionLimits.MAX_ID_LENGTH + 1))))
        assertIs<Decision.Reject>(OperationApplier.decide(state(), operation))
    }
    @Test fun `a rejection reason never exceeds the stored length`() {
        val id = "a".repeat(1_000)
        val decision = assertIs<Decision.Reject>(OperationApplier.decide(state(), op(StateChange.RecipeUnlocked(id))))
        assertEquals(ProgressionLimits.MAX_REASON_LENGTH, decision.reason.length)
    }
}
