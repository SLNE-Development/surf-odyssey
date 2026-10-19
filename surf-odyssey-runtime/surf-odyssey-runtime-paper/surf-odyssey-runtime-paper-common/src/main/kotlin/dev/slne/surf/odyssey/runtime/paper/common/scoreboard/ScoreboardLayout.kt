package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.buildText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Everything the scoreboard shows one player at one moment.
 *
 * @property playerName the name of the player
 * @property title the title the player shows, or `null`
 * @property quest the quest the player tracks, or `null`
 * @property ping the ping of the player in milliseconds
 * @property online the number of players on this server
 */
data class ScoreboardSnapshot(
    val playerName: String,
    val title: Component?,
    val quest: ScoreboardQuest?,
    val ping: Int,
    val online: Int,
)

/** Builds the banner and the lines of the scoreboard from a [ScoreboardSnapshot] and the current time. */
object ScoreboardLayout {
    /** The number of characters a line may have before it is shortened. */
    const val MAX_LINE_LENGTH = 28

    /** The number of lines the scoreboard can show at most. */
    const val MAX_LINES = 15

    /** The time zone of the clock and the date. */
    val ZONE: ZoneId = ZoneId.of("Europe/Berlin")

    /** The color the banner gradient starts with. */
    val GRADIENT_START: TextColor = TextColor.color(0x38BDF8)

    /** The color the banner gradient ends with. */
    val GRADIENT_END: TextColor = TextColor.color(0xA78BFA)

    private const val SEPARATOR_LENGTH = 14
    private val PLAIN = PlainTextComponentSerializer.plainText()
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Returns the banner text; the scoreboard API animates a gradient over it. */
    fun banner(): Component = Component.text("ODYSSEY".toSmallCaps(), null, TextDecoration.BOLD)

    /** Returns the line that divides two blocks. */
    fun separator(): Component = Component.text("─".repeat(SEPARATOR_LENGTH), NamedTextColor.DARK_GRAY)

    /**
     * Returns the lines that depend on the viewer: name and title, the quest, then ping and online players, with a
     * separator between two blocks. Blocks without data are left out.
     */
    fun viewerLines(snapshot: ScoreboardSnapshot): List<Component> {
        val blocks = listOfNotNull(identity(snapshot), quest(snapshot), footer(snapshot))
        return blocks.flatMapIndexed { index, block -> if (index == 0) block else listOf(separator()) + block }
    }

    /** Returns a separator followed by the time and the date of [now] in [ZONE]. */
    fun clockLines(now: ZonedDateTime): List<Component> {
        val local = now.withZoneSameInstant(ZONE)
        return listOf(
            separator(),
            labeled("Uhrzeit", buildText { variableValue(TIME_FORMAT.format(local)) }),
            labeled("Datum", buildText { variableValue(DATE_FORMAT.format(local)) }),
        )
    }

    /**
     * Returns [text] unchanged if it has at most [max] characters, otherwise its first `max - 1` characters
     * followed by an ellipsis, keeping the style of [text] itself.
     */
    fun shorten(text: Component, max: Int = MAX_LINE_LENGTH): Component {
        val plain = PLAIN.serialize(text)
        if (plain.length <= max) return text
        return Component.text(plain.take(max - 1) + "…").style(text.style())
    }

    /** A line made of a small-caps label, a colon and [value]. */
    private fun labeled(name: String, value: Component): Component = buildText {
        info("${name.toSmallCaps()}:")
        appendSpace()
        append(value)
    }

    /** The name line and, if the player shows a title, the title line. */
    private fun identity(snapshot: ScoreboardSnapshot): List<Component> = buildList {
        add(labeled("Spieler", buildText { variableValue(snapshot.playerName) }))
        val title = snapshot.title
        if (title != null && PLAIN.serialize(title).isNotEmpty()) {
            add(labeled("Titel", shorten(title, MAX_LINE_LENGTH - "Titel: ".length)))
        }
    }

    /** The quest header, name and objective, or `null` without a quest. */
    private fun quest(snapshot: ScoreboardSnapshot): List<Component>? {
        val quest = snapshot.quest ?: return null
        return listOf(
            buildText { info("${"Quest".toSmallCaps()}:") },
            shorten(quest.name),
            shorten(quest.objective),
        )
    }

    /** The ping and online lines. */
    private fun footer(snapshot: ScoreboardSnapshot): List<Component> = listOf(
        labeled("Ping", buildText { variableValue("${snapshot.ping} ms") }),
        labeled("Online", buildText { variableValue("${snapshot.online}") }),
    )
}
