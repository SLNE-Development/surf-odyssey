package dev.slne.surf.odyssey.runtime.paper.progression.state

import dev.slne.surf.odyssey.core.common.progression.CounterDeltaDto
import dev.slne.surf.odyssey.core.common.progression.CounterDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.core.common.progression.StateChange
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests for the cached progression of one player and how changes and counter deltas apply to it. */
class PlayerProgressionTest {
    /** A clock fixed at one second after the epoch. */
    private val clock = Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC)

    /** Creates the state of a player without any progression. */
    private fun empty() = ProgressionStateDto(
        UUID.randomUUID(), emptyList(), emptyList(), emptySet(), emptySet(), null, null, emptyList(), emptyList()
    )

    @Test
    fun `accepted quests advance and complete locally`() {
        val progression = PlayerProgression(empty(), clock)

        progression.apply(
            listOf(
                StateChange.QuestAccepted("q", stepVersion = 1, period = 0),
                StateChange.QuestStepAdvanced("q", fromStep = 0, toStep = 1, stepVersion = 2),
                StateChange.QuestCompleted("q", fromStep = 1),
            )
        )

        val quest = progression.questState("q")!!
        assertEquals(QuestStatus.COMPLETED, quest.status)
        assertEquals(1, quest.step)
        assertEquals(1_000L, progression.state.quests.single().completedAt)
    }

    @Test
    fun `recipes and titles unlock locally`() {
        val progression = PlayerProgression(empty(), clock)

        progression.apply(listOf(StateChange.RecipeUnlocked("sushi"), StateChange.TitleUnlocked("angler")))

        assertTrue(progression.isRecipeUnlocked("sushi"))
        assertTrue(progression.hasTitle("angler"))
        assertFalse(progression.hasTitle("other"))
        assertNull(progression.questState("q"))
    }

    @Test
    fun `counter deltas add to the cached counter`() {
        val progression = PlayerProgression(empty(), clock)
        val delta = CounterDeltaDto("q", step = 0, stepVersion = 1, objective = 0, period = 0, delta = 3)

        progression.addCounter(delta)
        progression.addCounter(delta)

        assertEquals(6, progression.counter("q", step = 0, stepVersion = 1, objective = 0, period = 0))
    }

    @Test
    fun `a reload keeps deltas that were not flushed yet`() {
        val progression = PlayerProgression(empty(), clock)
        val stored = CounterDto("q", step = 0, stepVersion = 1, objective = 0, period = 0, value = 4)
        val pending = CounterDeltaDto("q", step = 0, stepVersion = 1, objective = 0, period = 0, delta = 2)

        progression.replace(empty().copy(counters = listOf(stored)), listOf(pending))

        assertEquals(6, progression.counter("q", step = 0, stepVersion = 1, objective = 0, period = 0))
    }

    @Test
    fun `abandoning drops the counters of the quest and keeps others`() {
        val progression = PlayerProgression(empty(), clock)
        progression.apply(listOf(StateChange.QuestAccepted("q", 1, 0), StateChange.QuestAccepted("o", 1, 0)))
        progression.addCounter(CounterDeltaDto("q", 0, 1, 0, 0, 3))
        progression.addCounter(CounterDeltaDto("o", 0, 1, 0, 0, 2))

        progression.abandon("q")

        assertEquals(QuestStatus.ABANDONED, progression.questState("q")?.status)
        assertEquals(0, progression.counter("q", 0, 1, 0, 0))
        assertEquals(2, progression.counter("o", 0, 1, 0, 0))
    }

    @Test
    fun `abandoning a quest that is not active changes nothing`() {
        val progression = PlayerProgression(empty(), clock)
        progression.apply(listOf(StateChange.QuestAccepted("q", 1, 0), StateChange.QuestCompleted("q", 0)))

        progression.abandon("q")

        assertEquals(QuestStatus.COMPLETED, progression.questState("q")?.status)
    }

    @Test
    fun `the tracked quest is cached`() {
        val progression = PlayerProgression(empty(), clock)

        progression.setTrackedQuest("q")
        assertEquals("q", progression.state.trackedQuest)
        progression.setTrackedQuest(null)
        assertNull(progression.state.trackedQuest)
    }
}
