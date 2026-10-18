package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType.DAILY
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType.MONTHLY
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType.WEEKLY
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for period boundaries in Europe/Berlin. */
class PeriodsTest {
    private fun at(s: String) = ZonedDateTime.parse(s).toInstant()

    @Test
    fun `daily changes exactly at local midnight`() {
        assertEquals(
            Periods.index(DAILY, at("2026-10-18T23:59:59+02:00")) + 1,
            Periods.index(DAILY, at("2026-10-19T00:00:00+02:00"))
        )
    }

    @Test
    fun `dst change keeps one period per day`() {
        val before = Periods.index(DAILY, at("2026-10-25T01:00:00+02:00"))
        val after = Periods.index(DAILY, at("2026-10-25T23:00:00+01:00"))
        assertEquals(before, after)
    }

    @Test
    fun `weekly starts on monday`() {
        assertEquals(
            Periods.index(WEEKLY, at("2026-10-18T12:00:00+02:00")) + 1,
            Periods.index(WEEKLY, at("2026-10-19T00:00:00+02:00"))
        )
    }

    @Test
    fun `monthly starts on the first`() {
        assertEquals(
            Periods.index(MONTHLY, at("2026-10-31T23:00:00+01:00")) + 1,
            Periods.index(MONTHLY, at("2026-11-01T00:00:00+01:00"))
        )
    }

    @Test
    fun `end of a period is the start of the next`() {
        val i = Periods.index(DAILY, at("2026-10-18T12:00:00+02:00"))
        assertEquals(i + 1, Periods.index(DAILY, Periods.endOf(DAILY, i)))
    }

    @Test
    fun `end of weekly and monthly periods is the start of the next`() {
        val w = Periods.index(WEEKLY, at("2026-10-18T12:00:00+02:00"))
        assertEquals(w + 1, Periods.index(WEEKLY, Periods.endOf(WEEKLY, w)))
        assertEquals(at("2026-10-19T00:00:00+02:00"), Periods.endOf(WEEKLY, w))
        val m = Periods.index(MONTHLY, at("2026-10-18T12:00:00+02:00"))
        assertEquals(m + 1, Periods.index(MONTHLY, Periods.endOf(MONTHLY, m)))
        assertEquals(at("2026-11-01T00:00:00+01:00"), Periods.endOf(MONTHLY, m))
    }
}
