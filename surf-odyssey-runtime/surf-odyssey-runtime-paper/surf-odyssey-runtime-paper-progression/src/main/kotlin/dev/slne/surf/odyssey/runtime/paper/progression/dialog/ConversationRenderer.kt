@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.AnswerKind
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import io.papermc.paper.dialog.Dialog
import net.kyori.adventure.nbt.api.BinaryTagHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import java.util.*

/**
 * One answer line of a conversation screen.
 *
 * @property text the answer text
 * @property tooltip the hover text of the line
 * @property kind how the line is coloured
 * @property onClick called on the player's thread with the player who clicked the line
 */
class ConversationLine(
    val text: Component,
    val tooltip: Component,
    val kind: AnswerKind,
    val onClick: suspend (Player) -> Unit,
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
 *
 * Answer lines and the button send a custom click with the identifier [AnswerPayload.KEY] and a payload of the
 * screen token and the line index, [AnswerPayload.CLOSE] for the button.
 */
object ConversationRenderer {
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
     * @param token the token the clicks of this dialog carry
     * @return a new dialog
     */
    fun render(screen: ConversationScreen, token: UUID): Dialog = dialog {
        base {
            title(screen.title)
            body {
                plainMessage(screen.portrait)
                plainMessage(screen.npcName)
                screen.meta?.let { plainMessage(it.colorIfAbsent(META_COLOR)) }
                plainMessage(screen.text, TEXT_WIDTH)
                screen.answers.forEachIndexed { index, line ->
                    plainMessage(answerLine(index, line, token), TEXT_WIDTH)
                }
            }
        }
        type {
            notice {
                label { text("Gespräch beenden") }
                action { customClick(AnswerPayload.KEY, payload(token, AnswerPayload.CLOSE)) }
            }
        }
    }

    /**
     * Builds the clickable text of an answer, numbered from 1.
     *
     * @param index the index of the answer in the screen
     * @param line the answer
     * @param token the token of the screen
     */
    private fun answerLine(index: Int, line: ConversationLine, token: UUID): Component = buildText {
        text("${index + 1}. ", NUMBER_COLOR)
        append(line.text.colorIfAbsent(colorOf(line.kind)))
    }.hoverEvent(line.tooltip).clickEvent(ClickEvent.custom(AnswerPayload.KEY, payload(token, index)))

    /** Returns the click payload of line [index] of the screen with [token]. */
    private fun payload(token: UUID, index: Int): BinaryTagHolder =
        BinaryTagHolder.binaryTagHolder(AnswerPayload.encode(token, index))
}
