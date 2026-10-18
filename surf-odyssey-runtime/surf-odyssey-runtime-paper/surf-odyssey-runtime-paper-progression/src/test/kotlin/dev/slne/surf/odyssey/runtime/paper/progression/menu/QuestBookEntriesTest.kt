package dev.slne.surf.odyssey.runtime.paper.progression.menu

import dev.slne.surf.odyssey.core.common.progression.CounterDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStateDto
import dev.slne.surf.odyssey.core.common.progression.QuestStatus.ABANDONED
import dev.slne.surf.odyssey.core.common.progression.QuestStatus.ACTIVE
import dev.slne.surf.odyssey.core.common.progression.QuestStatus.COMPLETED
import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.RecurringPools
import dev.slne.surf.odyssey.runtime.paper.progression.quest.Periods
import dev.slne.surf.odyssey.runtime.paper.progression.quest.RecurringDraw
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for building the sections of the quest book from the registry, the player's state and the time. */
class QuestBookEntriesTest {
    private val player = UUID.fromString("3f1e9c2a-7b44-4d0e-9a51-2c6d8e0f4b17")

    /** Sunday 2026-10-18, 10:00 in Berlin (UTC+2). */
    private val now = Instant.parse("2026-10-18T08:00:00Z")
    private val today = Periods.index(QuestType.DAILY, now)

    private fun state(quests: List<QuestStateDto>, counters: List<CounterDto> = emptyList(), tracked: String? = null) =
        ProgressionStateDto(player, quests, counters, emptySet(), emptySet(), null, tracked, emptyList(), emptyList())

    private fun sideQuests(count: Int) = TestPacks.TestPack(
        npcs = listOf(TestPacks.brina),
        quests = (1..count).map { TestPacks.quest("side_$it", QuestType.SIDE) },
        pools = RecurringPools(dailyCount = 0, weeklyCount = 0, monthlyCount = 0),
    )

    @Test
    fun `abandoned quests do not appear`() {
        val registry = ProgressionRegistry.build(sideQuests(2))
        val book = QuestBookEntries.build(
            registry,
            state(listOf(QuestStateDto("side_1", ABANDONED, 0, 1, 0, null), QuestStateDto("side_2", ACTIVE, 0, 1, 0, null))),
            now,
        )

        assertEquals(listOf("side_2"), book.active.map { it.quest.id })
        assertEquals(0, book.completedCount)
        assertTrue(book.recentlyCompleted.isEmpty())
    }

    @Test
    fun `the main quest cannot be abandoned`() {
        val registry = ProgressionRegistry.build(TestPacks.valid())
        val drawn = RecurringDraw.draw(player, QuestType.DAILY, today, registry.pool(QuestType.DAILY), 2).first()
        val book = QuestBookEntries.build(
            registry,
            state(
                listOf(
                    QuestStateDto("harbor_intro", ACTIVE, 0, 1, 0, null),
                    QuestStateDto(drawn.id, ACTIVE, 0, 1, today, null),
                )
            ),
            now,
        )

        val main = book.active.single { it.quest.id == "harbor_intro" }
        val daily = book.active.single { it.quest.id == drawn.id }
        assertFalse(main.abandonable)
        assertTrue(daily.abandonable)
    }

    @Test
    fun `active quests show the counts of the current step and the tracked mark`() {
        val registry = ProgressionRegistry.build(sideQuests(2))
        val book = QuestBookEntries.build(
            registry,
            state(
                listOf(QuestStateDto("side_1", ACTIVE, 0, 1, 0, null), QuestStateDto("side_2", ACTIVE, 0, 1, 0, null)),
                counters = listOf(CounterDto("side_1", 0, 1, 0, 0, 1)),
                tracked = "side_2",
            ),
            now,
        )

        assertEquals(listOf(1), book.active.single { it.quest.id == "side_1" }.progress.counts)
        assertEquals(listOf("side_2"), book.active.filter { it.tracked }.map { it.quest.id })
    }

    @Test
    fun `recurring quests show the time until the end of their period`() {
        val registry = ProgressionRegistry.build(TestPacks.valid())
        val book = QuestBookEntries.build(registry, state(emptyList()), now)

        val daily = book.recurring.filter { it.quest.type == QuestType.DAILY }
        assertEquals(2, daily.size)
        // The day ends at 2026-10-19 00:00 in Berlin, which is 2026-10-18T22:00Z.
        assertTrue(daily.all { it.remaining == Duration.ofHours(14) })
        // The week ends on Monday 2026-10-19 00:00 in Berlin.
        assertEquals(Duration.ofHours(14), book.recurring.single { it.quest.type == QuestType.WEEKLY }.remaining)
        // The month ends on 2026-11-01 00:00 in Berlin, which is 2026-10-31T23:00Z after the clock change.
        assertEquals(
            Duration.ofDays(13).plusHours(15),
            book.recurring.single { it.quest.type == QuestType.MONTHLY }.remaining,
        )
    }

    @Test
    fun `recurring quests show their status in the current period`() {
        val registry = ProgressionRegistry.build(TestPacks.valid())
        val (first, second) = RecurringDraw.draw(player, QuestType.DAILY, today, registry.pool(QuestType.DAILY), 2)
        val weekly = registry.pool(QuestType.WEEKLY).single()
        val monthly = registry.pool(QuestType.MONTHLY).single()
        val book = QuestBookEntries.build(
            registry,
            state(
                listOf(
                    QuestStateDto(first.id, ACTIVE, 0, 1, today, null),
                    QuestStateDto(second.id, COMPLETED, 0, 1, today, 1L),
                    QuestStateDto(weekly.id, ACTIVE, 0, 1, Periods.index(QuestType.WEEKLY, now) - 1, null),
                    QuestStateDto(monthly.id, ABANDONED, 0, 1, Periods.index(QuestType.MONTHLY, now), null),
                )
            ),
            now,
        )

        val status = book.recurring.associate { it.quest.id to it.status }
        assertEquals(RecurringStatus.ACTIVE, status[first.id])
        assertEquals(RecurringStatus.COMPLETED, status[second.id])
        assertEquals(RecurringStatus.OPEN, status[weekly.id])
        assertEquals(RecurringStatus.OPEN, status[monthly.id])
        assertFalse(book.active.any { it.quest.id == weekly.id })
    }

    @Test
    fun `completed quests are counted and the ten latest are listed newest first`() {
        val registry = ProgressionRegistry.build(sideQuests(12))
        val book = QuestBookEntries.build(
            registry,
            state((1..12).map { QuestStateDto("side_$it", COMPLETED, 0, 1, 0, it.toLong()) }),
            now,
        )

        assertEquals(12, book.completedCount)
        assertEquals((12 downTo 3).map { "side_$it" }, book.recentlyCompleted.map { it.id })
    }
}
