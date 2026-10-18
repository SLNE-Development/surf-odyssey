package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Maps instants to the numbered periods of recurring quests, measured in local Europe/Berlin time.
 *
 * Daily periods start at local midnight, weekly periods on Monday and monthly periods on the first of the month.
 */
object Periods {
    /** Time zone in which period boundaries are evaluated. */
    val ZONE: ZoneId = ZoneId.of("Europe/Berlin")

    /** Epoch day of the Monday that starts weekly period 0 (1969-12-29). */
    private const val WEEK_ZERO_EPOCH_DAY = -3L

    /**
     * Returns the number of the period of [type] that contains [instant].
     *
     * Daily periods count local days since 1970-01-01, weekly periods count weeks since Monday 1969-12-29 and monthly
     * periods are `year * 12 + month - 1`. Types that do not recur always yield 0.
     *
     * @param type the quest type
     * @param instant the point in time
     * @return the period index
     */
    fun index(type: QuestType, instant: Instant): Long {
        val date = instant.atZone(ZONE).toLocalDate()
        return when (type) {
            QuestType.DAILY -> date.toEpochDay()
            QuestType.WEEKLY -> Math.floorDiv(date.toEpochDay() - WEEK_ZERO_EPOCH_DAY, 7L)
            QuestType.MONTHLY -> date.year * 12L + date.monthValue - 1
            QuestType.MAIN, QuestType.SIDE -> 0L
        }
    }

    /**
     * Returns the start of the period after [index], which is the end of period [index].
     *
     * Types that do not recur never end and yield [Instant.MAX].
     *
     * @param type the quest type
     * @param index the period index as returned by [index]
     * @return the first instant of the next period
     */
    fun endOf(type: QuestType, index: Long): Instant {
        val nextStart: LocalDate = when (type) {
            QuestType.DAILY -> LocalDate.ofEpochDay(index + 1)
            QuestType.WEEKLY -> LocalDate.ofEpochDay(WEEK_ZERO_EPOCH_DAY + (index + 1) * 7)
            QuestType.MONTHLY -> LocalDate.of(Math.floorDiv(index + 1, 12L).toInt(), Math.floorMod(index + 1, 12L).toInt() + 1, 1)
            QuestType.MAIN, QuestType.SIDE -> return Instant.MAX
        }
        return nextStart.atStartOfDay(ZONE).toInstant()
    }
}
