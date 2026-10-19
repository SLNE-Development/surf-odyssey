package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

import dev.slne.surf.api.core.font.toSmallCaps
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for the lines of the scoreboard. */
class ScoreboardLayoutTest {
    private fun plain(component: Component) = PlainTextComponentSerializer.plainText().serialize(component)

    private fun viewer(snapshot: ScoreboardSnapshot) = ScoreboardLayout.viewerLines(snapshot).map(::plain)

    private fun clock(now: ZonedDateTime) = ScoreboardLayout.clockLines(now).map(::plain)

    private fun label(name: String) = name.toSmallCaps()

    private val separator = plain(ScoreboardLayout.separator())

    private val quest = ScoreboardQuest(Component.text("Leuchtfeuer"), Component.text("Sammle Holz 3/12"))

    @Test
    fun `shows name, title, quest, ping and online players divided by separators`() {
        val lines = viewer(ScoreboardSnapshot("Timonso", Component.text("Seebaer"), quest, ping = 23, online = 12))

        assertEquals(
            listOf(
                "${label("Spieler")}: Timonso", "${label("Titel")}: Seebaer", separator,
                "${label("Quest")}:", "Leuchtfeuer", "Sammle Holz 3/12", separator,
                "${label("Ping")}: 23 ms", "${label("Online")}: 12"
            ),
            lines
        )
    }

    @Test
    fun `leaves out title and quest without a provider and keeps a single separator`() {
        val lines = viewer(ScoreboardSnapshot("Timonso", null, null, ping = 5, online = 1))

        assertEquals(
            listOf("${label("Spieler")}: Timonso", separator, "${label("Ping")}: 5 ms", "${label("Online")}: 1"),
            lines
        )
    }

    @Test
    fun `leaves out an empty title`() {
        val lines = viewer(ScoreboardSnapshot("Timonso", Component.empty(), null, ping = 5, online = 1))

        assertEquals(
            listOf("${label("Spieler")}: Timonso", separator, "${label("Ping")}: 5 ms", "${label("Online")}: 1"),
            lines
        )
    }

    @Test
    fun `shortens a long quest line with an ellipsis`() {
        val long = ScoreboardQuest(Component.text("a".repeat(40)), Component.text("b".repeat(28)))
        val lines = viewer(ScoreboardSnapshot("Timonso", null, long, ping = 5, online = 1))

        assertEquals("a".repeat(27) + "…", lines[3])
        assertEquals("b".repeat(28), lines[4])
    }

    @Test
    fun `keeps a text that is exactly as long as the limit`() {
        val text = Component.text("x".repeat(ScoreboardLayout.MAX_LINE_LENGTH))

        assertEquals(text, ScoreboardLayout.shorten(text))
    }

    @Test
    fun `shows the banner as the small-caps name`() {
        assertEquals("ODYSSEY".toSmallCaps(), plain(ScoreboardLayout.banner()))
    }

    @Test
    fun `shows time and date in Berlin in summer time`() {
        val now = ZonedDateTime.of(2026, 10, 19, 14, 5, 9, 0, ZoneId.of("UTC"))

        assertEquals(listOf(separator, "${label("Uhrzeit")}: 16:05:09", "${label("Datum")}: 19.10.2026"), clock(now))
    }

    @Test
    fun `shows the next day in Berlin when it is already past midnight there`() {
        val now = ZonedDateTime.of(2026, 10, 19, 23, 30, 0, 0, ZoneId.of("UTC"))

        assertEquals(listOf(separator, "${label("Uhrzeit")}: 01:30:00", "${label("Datum")}: 20.10.2026"), clock(now))
    }

    @Test
    fun `shows winter time with one hour offset`() {
        val now = ZonedDateTime.of(2026, 12, 1, 12, 0, 0, 0, ZoneId.of("UTC"))

        assertEquals(listOf(separator, "${label("Uhrzeit")}: 13:00:00", "${label("Datum")}: 01.12.2026"), clock(now))
    }

    @Test
    fun `never needs more lines than the scoreboard allows`() {
        val long = ScoreboardQuest(Component.text("a".repeat(80)), Component.text("b".repeat(80)))
        val snapshot = ScoreboardSnapshot("Timonso", Component.text("t".repeat(80)), long, ping = 999, online = 999)
        val now = ZonedDateTime.now(ScoreboardLayout.ZONE)

        val total = 1 + ScoreboardLayout.viewerLines(snapshot).size + ScoreboardLayout.clockLines(now).size

        assertTrue(total <= ScoreboardLayout.MAX_LINES, "needs $total lines")
    }
}
