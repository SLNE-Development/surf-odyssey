package dev.slne.surf.odyssey.runtime.paper.polartest.check

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks that [CheckReport] counts every expectation and keeps a readable message for each failed one. */
class CheckReportTest {
    @Test
    fun `a report without failures passes and counts its checks`() {
        val report = CheckReport("setup")
        report.expect("Version", 1, 1)
        report.expectTrue("Möbel platziert", true)
        assertTrue(report.passed)
        assertEquals(2, report.checks)
    }

    @Test
    fun `a mismatch names what was expected and what was found`() {
        val report = CheckReport("migrate-raw")
        report.expect("Version", 2, 1)
        assertFalse(report.passed)
        assertEquals(listOf("Version: erwartet <2>, gefunden <1>"), report.failureMessages)
    }

    @Test
    fun `fail and expectTrue add their message`() {
        val report = CheckReport("fail-newer")
        report.fail("Abgebrochen")
        report.expectTrue("Welt nicht geladen", false)
        assertEquals(listOf("Abgebrochen", "Welt nicht geladen"), report.failureMessages)
        assertEquals(2, report.checks)
    }
}
