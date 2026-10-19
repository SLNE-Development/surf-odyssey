package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.sidebar.SidebarLine
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

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

/** Builds the title and the lines of the scoreboard from a [ScoreboardSnapshot]. */
object ScoreboardLayout {
    /** The number of characters a line may have before it is shortened. */
    const val MAX_LINE_LENGTH = 28

    private val PLAIN = PlainTextComponentSerializer.plainText()

    /** Returns the title of the scoreboard. */
    fun title(): Component = buildText { variableValue("Odyssey") }

    /**
     * Returns the lines for [snapshot]: name and title, the quest, then ping and online players, each block
     * preceded by an empty line. Blocks without data are left out.
     */
    fun lines(snapshot: ScoreboardSnapshot): List<SidebarLine> {
        val blocks = listOfNotNull(identity(snapshot), quest(snapshot), footer(snapshot))
        return blocks.flatMap { listOf(SidebarLine(Component.empty())) + it }
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

    /** The name line and, if the player shows a title, the title line. */
    private fun identity(snapshot: ScoreboardSnapshot): List<SidebarLine> = buildList {
        add(SidebarLine(buildText {
            info("Spieler:")
            appendSpace()
            variableValue(snapshot.playerName)
        }))
        val title = snapshot.title
        if (title != null && PLAIN.serialize(title).isNotEmpty()) {
            add(SidebarLine(buildText {
                info("Titel:")
                appendSpace()
                append(shorten(title))
            }))
        }
    }

    /** The quest header, name and objective, or `null` without a quest. */
    private fun quest(snapshot: ScoreboardSnapshot): List<SidebarLine>? {
        val quest = snapshot.quest ?: return null
        return listOf(
            SidebarLine(buildText { info("Quest:") }),
            SidebarLine(shorten(quest.name)),
            SidebarLine(shorten(quest.objective)),
        )
    }

    /** The ping and online lines. */
    private fun footer(snapshot: ScoreboardSnapshot): List<SidebarLine> = listOf(
        SidebarLine(buildText {
            info("Ping:")
            appendSpace()
            variableValue("${snapshot.ping} ms")
        }),
        SidebarLine(buildText {
            info("Online:")
            appendSpace()
            variableValue("${snapshot.online}")
        }),
    )
}
