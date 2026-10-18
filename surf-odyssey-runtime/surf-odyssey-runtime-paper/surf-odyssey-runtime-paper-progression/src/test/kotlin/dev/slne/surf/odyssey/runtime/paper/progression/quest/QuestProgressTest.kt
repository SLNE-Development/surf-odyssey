package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.core.common.progression.CounterDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStatus.ABANDONED
import dev.slne.surf.odyssey.core.common.progression.QuestStatus.ACTIVE
import dev.slne.surf.odyssey.core.common.progression.QuestStatus.COMPLETED
import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks.StepSpec
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.Objective
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import org.bukkit.Material
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests for computing quest progress from stored counters. */
class QuestProgressTest {
    private fun state(quests: List<QuestStateDto>) = ProgressionStateDto(
        UUID.randomUUID(), quests, emptyList(), emptySet(), emptySet(), null, null, emptyList(), emptyList()
    )

    @Test
    fun `counters of an older step version count as zero`() {
        val q = TestPacks.questWithVersion(stepVersion = 2)
        val p = assertNotNull(
            QuestProgressView.compute(
                q, QuestStateDto(q.id, ACTIVE, 0, 2, 0, null),
                listOf(CounterDto(q.id, 0, 1, 0, 0, 5)), period = 0
            )
        )
        assertEquals(listOf(0), p.counts)
    }

    @Test
    fun `counters of the current step version count`() {
        val q = TestPacks.questWithVersion(stepVersion = 2)
        val p = assertNotNull(
            QuestProgressView.compute(
                q, QuestStateDto(q.id, ACTIVE, 0, 1, 0, null),
                listOf(CounterDto(q.id, 0, 2, 0, 0, 5), CounterDto(q.id, 0, 1, 0, 0, 3)), period = 0
            )
        )
        assertEquals(listOf(5), p.counts)
        assertFalse(p.complete)
    }

    @Test
    fun `reached steps stay when a later step changes`() {
        val harvest = listOf(Objective.Harvest(Material.WHEAT, 12, null))
        val q = TestPacks.questWithSteps(steps = listOf(StepSpec(1, harvest), StepSpec(3, harvest)))
        val p = assertNotNull(
            QuestProgressView.compute(
                q, QuestStateDto(q.id, ACTIVE, 1, 2, 0, null),
                listOf(CounterDto(q.id, 1, 2, 0, 0, 7), CounterDto(q.id, 0, 1, 0, 0, 12)), period = 0
            )
        )
        assertEquals(1, p.step)
        assertEquals(listOf(0), p.counts)
    }

    @Test
    fun `recurring state of an old period is ignored`() {
        val q = TestPacks.questWithVersion(stepVersion = 1, type = QuestType.DAILY)
        val stale = QuestStateDto(q.id, ACTIVE, 0, 1, 4, null)
        assertNull(QuestProgressView.compute(q, stale, listOf(CounterDto(q.id, 0, 1, 0, 4, 3)), period = 5))
        val current = assertNotNull(
            QuestProgressView.compute(q, stale.copy(period = 5), listOf(CounterDto(q.id, 0, 1, 0, 5, 3)), period = 5)
        )
        assertEquals(listOf(3), current.counts)
    }

    @Test
    fun `counts are capped at the objective amount`() {
        val q = TestPacks.questWithVersion(stepVersion = 1, amount = 12)
        val p = assertNotNull(
            QuestProgressView.compute(
                q, QuestStateDto(q.id, ACTIVE, 0, 1, 0, null),
                listOf(CounterDto(q.id, 0, 1, 0, 0, 15)), period = 0
            )
        )
        assertEquals(listOf(12), p.counts)
        assertTrue(p.complete)
    }

    @Test
    fun `only active quests have progress`() {
        val q = TestPacks.questWithVersion(stepVersion = 1)
        assertNull(QuestProgressView.compute(q, null, emptyList(), period = 0))
        assertNull(QuestProgressView.compute(q, QuestStateDto(q.id, COMPLETED, 0, 1, 0, 5L), emptyList(), 0))
        assertNull(QuestProgressView.compute(q, QuestStateDto(q.id, ABANDONED, 0, 1, 0, null), emptyList(), 0))
        assertNull(QuestProgressView.compute(q, QuestStateDto(q.id, ACTIVE, 3, 1, 0, null), emptyList(), 0))
    }

    @Test
    fun `active quests skip recurring states of an old period and unknown quests`() {
        val registry = ProgressionRegistry.build(TestPacks.valid())
        val now = Instant.parse("2026-03-10T12:00:00Z")
        val today = Periods.index(QuestType.DAILY, now)
        val active = QuestProgressView.activeQuests(
            registry, state(
                listOf(
                    QuestStateDto("d1", ACTIVE, 0, 1, today - 1, null),
                    QuestStateDto("d2", ACTIVE, 0, 1, today, null),
                    QuestStateDto("gone", ACTIVE, 0, 1, 0, null),
                    QuestStateDto("harbor_intro", COMPLETED, 0, 1, 0, 1L),
                )
            ), now
        )
        assertEquals(listOf("d2"), active.map { it.quest.id })
        assertEquals(today, active.single().period)
    }

    @Test
    fun `the next tracked quest is the first main quest, otherwise the first accepted one`() {
        val registry = ProgressionRegistry.build(TestPacks.valid())
        val now = Instant.parse("2026-03-10T12:00:00Z")
        val today = Periods.index(QuestType.DAILY, now)
        val d2 = QuestStateDto("d2", ACTIVE, 0, 1, today, null)
        val w1 = QuestStateDto("w1", ACTIVE, 0, 1, Periods.index(QuestType.WEEKLY, now), null)
        val main = QuestStateDto("harbor_intro", ACTIVE, 0, 1, 0, null)

        assertEquals("harbor_intro", QuestProgressView.nextTracked(registry, state(listOf(d2, w1, main)), now)?.id)
        assertEquals("d2", QuestProgressView.nextTracked(registry, state(listOf(d2, w1)), now)?.id)
        assertNull(QuestProgressView.nextTracked(registry, state(listOf(main.copy(status = COMPLETED))), now))
    }

    @Test
    fun `a player tracks an active quest only while the tracked quest still counts`() {
        val registry = ProgressionRegistry.build(TestPacks.valid())
        val now = Instant.parse("2026-03-10T12:00:00Z")
        val today = Periods.index(QuestType.DAILY, now)
        val main = QuestStateDto("harbor_intro", ACTIVE, 0, 1, 0, null)
        val staleDaily = QuestStateDto("d1", ACTIVE, 0, 1, today - 1, null)

        assertTrue(QuestProgressView.tracksActiveQuest(registry, state(listOf(main)).copy(trackedQuest = "harbor_intro"), now))
        assertFalse(QuestProgressView.tracksActiveQuest(registry, state(listOf(main)), now))
        assertFalse(QuestProgressView.tracksActiveQuest(registry, state(listOf(staleDaily)).copy(trackedQuest = "d1"), now))
        assertFalse(QuestProgressView.tracksActiveQuest(registry, state(emptyList()).copy(trackedQuest = "gone"), now))
        assertFalse(
            QuestProgressView.tracksActiveQuest(
                registry, state(listOf(main.copy(status = COMPLETED))).copy(trackedQuest = "harbor_intro"), now
            )
        )
    }
}
