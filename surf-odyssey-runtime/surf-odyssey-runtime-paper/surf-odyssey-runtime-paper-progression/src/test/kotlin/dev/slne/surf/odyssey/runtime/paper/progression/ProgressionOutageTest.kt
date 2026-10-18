package dev.slne.surf.odyssey.runtime.paper.progression

import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Tests that failures of the progression service are logged once per outage. */
class ProgressionOutageTest {
    /** Every record written to [logger]. */
    private val records = mutableListOf<LogRecord>()

    /** A logger that writes every level into [records] only. */
    private val logger = Logger.getAnonymousLogger().apply {
        useParentHandlers = false
        level = Level.ALL
        addHandler(object : Handler() {
            /** Adds the record to [records]. */
            override fun publish(record: LogRecord) {
                records += record
            }

            /** Does nothing, as nothing is buffered. */
            override fun flush() = Unit

            /** Does nothing, as nothing is held open. */
            override fun close() = Unit
        })
    }

    /** Leaves the service marked available for the other tests. */
    @AfterTest
    fun recover() {
        Progression.markAvailable()
    }

    @Test
    fun `only the first failure of an outage is a warning`() {
        repeat(3) { Progression.reportFailure(logger, "send counter deltas", IllegalStateException("down")) }

        val warnings = records.filter { it.level == Level.WARNING }
        assertEquals(1, warnings.size)
        assertNull(warnings.single().thrown)
        assertFalse(Progression.isAvailable)
    }

    @Test
    fun `every failure keeps its stack trace at fine level`() {
        repeat(2) { Progression.reportFailure(logger, "send counter deltas", IllegalStateException("down")) }

        val fine = records.filter { it.level == Level.FINE }
        assertEquals(2, fine.size)
        fine.forEach { assertNotNull(it.thrown) }
    }

    @Test
    fun `a new outage after a recovery warns again`() {
        Progression.reportFailure(logger, "load", IllegalStateException("down"))
        Progression.markAvailable()
        Progression.reportFailure(logger, "load", IllegalStateException("down"))

        assertEquals(2, records.count { it.level == Level.WARNING })
    }
}
