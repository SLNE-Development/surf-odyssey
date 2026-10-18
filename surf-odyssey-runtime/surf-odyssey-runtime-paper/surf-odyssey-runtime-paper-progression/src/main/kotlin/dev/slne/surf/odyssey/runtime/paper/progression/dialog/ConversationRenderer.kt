@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.AnswerKind
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import io.papermc.paper.dialog.Dialog
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import java.time.Duration

/**
 * One answer line of a conversation screen.
 *
 * @property text the answer text
 * @property tooltip the hover text of the line
 * @property kind how the line is coloured
 * @property onClick called with the player who clicked the line
 */
class ConversationLine(
    val text: Component,
    val tooltip: Component,
    val kind: AnswerKind,
    val onClick: (Player) -> Unit,
)

/**
 * The content of one conversation dialog.
 *
 * @property title the dialog title
 * @property portrait the portrait glyph of the speaking NPC
 * @property npcName the name of the speaking NPC
 * @property meta the grey meta line, or `null` for none
 * @property text the narrated text
 * @property answers the answer lines, numbered from 1 in this order
 */
class ConversationScreen(
    val title: Component,
    val portrait: Component,
    val npcName: Component,
    val meta: Component?,
    val text: Component,
    val answers: List<ConversationLine>,
)

/**
 * Builds conversation dialogs: a notice dialog with the portrait, the NPC name, the meta line, the narrated text and
 * the answers as clickable, numbered text lines with a tooltip, and one button "Gespräch beenden" that closes it.
 * Every call builds a new dialog whose answer lines can each be clicked once.
 */
object ConversationRenderer {
    /** How long the answer lines of a shown dialog stay clickable. */
    private val ANSWER_LIFETIME: Duration = Duration.ofMinutes(15)

    /** The width of the narrated text and the answer lines. */
    private const val TEXT_WIDTH = 300

    /** The colour of the number in front of each answer. */
    private val NUMBER_COLOR = TextColor.color(0x555555)

    /** The colour of the meta line. */
    private val META_COLOR = TextColor.color(0xAAAAAA)

    /**
     * Returns the colour of answers of [kind]: gold for normal answers, green for accepting and grey for leaving.
     *
     * @param kind the answer kind
     */
    fun colorOf(kind: AnswerKind): TextColor = when (kind) {
        AnswerKind.NORMAL -> TextColor.color(0xFFAA00)
        AnswerKind.ACCEPT -> TextColor.color(0x55FF55)
        AnswerKind.LEAVE -> TextColor.color(0xAAAAAA)
    }

    /**
     * Returns the title of conversations about a quest of [type]: the type label in the type colour.
     *
     * @param type the quest type
     */
    fun questTitle(type: QuestType): Component {
        val (label, color) = when (type) {
            QuestType.MAIN -> "Hauptquest" to 0xFFAA00
            QuestType.SIDE -> "Nebenquest" to 0xFFFF55
            QuestType.DAILY -> "Daily-Quest" to 0x55FF55
            QuestType.WEEKLY -> "Weekly-Quest" to 0x55FFFF
            QuestType.MONTHLY -> "Monthly-Quest" to 0xFF55FF
        }
        return Component.text(label, TextColor.color(color))
    }

    /**
     * Builds the dialog of [screen].
     *
     * @param screen the content
     * @return a new dialog
     */
    fun render(screen: ConversationScreen): Dialog = dialog {
        base {
            title(screen.title)
            body {
                plainMessage(screen.portrait)
                plainMessage(screen.npcName)
                screen.meta?.let { plainMessage(it.colorIfAbsent(META_COLOR)) }
                plainMessage(screen.text, TEXT_WIDTH)
                screen.answers.forEachIndexed { index, line -> plainMessage(answerLine(index + 1, line), TEXT_WIDTH) }
            }
        }
        type {
            notice {
                label { text("Gespräch beenden") }
            }
        }
    }

    /**
     * Builds the clickable text of an answer.
     *
     * @param number the number shown in front of the answer
     * @param line the answer
     */
    private fun answerLine(number: Int, line: ConversationLine): Component {
        val options = ClickCallback.Options.builder().uses(1).lifetime(ANSWER_LIFETIME).build()
        val callback = ClickCallback<Audience> { audience -> if (audience is Player) line.onClick(audience) }
        return buildText {
            text("$number. ", NUMBER_COLOR)
            append(line.text.colorIfAbsent(colorOf(line.kind)))
        }.hoverEvent(line.tooltip).clickEvent(ClickEvent.callback(callback, options))
    }
}
