package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for the lines of the scoreboard. */
class ScoreboardLayoutTest {
    private fun plain(component: Component) = PlainTextComponentSerializer.plainText().serialize(component)

    private fun plainLines(snapshot: ScoreboardSnapshot) = ScoreboardLayout.lines(snapshot).map { plain(it.text) }

    private val quest = ScoreboardQuest(Component.text("Leuchtfeuer"), Component.text("Sammle Holz 3/12"))

    @Test
    fun `shows name, title, quest, ping and online players`() {
        val lines = plainLines(
            ScoreboardSnapshot("Timonso", Component.text("Seebaer"), quest, ping = 23, online = 12)
        )

        assertEquals(
            listOf(
                "", "Spieler: Timonso", "Titel: Seebaer",
                "", "Quest:", "Leuchtfeuer", "Sammle Holz 3/12",
                "", "Ping: 23 ms", "Online: 12"
            ),
            lines
        )
    }

    @Test
    fun `leaves out title and quest without a provider`() {
        val lines = plainLines(ScoreboardSnapshot("Timonso", null, null, ping = 5, online = 1))

        assertEquals(listOf("", "Spieler: Timonso", "", "Ping: 5 ms", "Online: 1"), lines)
    }

    @Test
    fun `leaves out an empty title`() {
        val lines = plainLines(ScoreboardSnapshot("Timonso", Component.empty(), null, ping = 5, online = 1))

        assertEquals(listOf("", "Spieler: Timonso", "", "Ping: 5 ms", "Online: 1"), lines)
    }

    @Test
    fun `shortens a long quest line with an ellipsis`() {
        val long = ScoreboardQuest(Component.text("a".repeat(40)), Component.text("b".repeat(28)))
        val lines = plainLines(ScoreboardSnapshot("Timonso", null, long, ping = 5, online = 1))

        assertEquals("a".repeat(27) + "…", lines[4])
        assertEquals("b".repeat(28), lines[5])
    }

    @Test
    fun `keeps a text that is exactly as long as the limit`() {
        val text = Component.text("x".repeat(ScoreboardLayout.MAX_LINE_LENGTH))

        assertEquals(text, ScoreboardLayout.shorten(text))
    }
}
