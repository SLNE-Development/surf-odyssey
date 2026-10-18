package dev.slne.surf.odyssey.runtime.paper.content.quests

import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.AnswerKind
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.DialogNodeBuilder
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.NodeAction
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import net.kyori.adventure.text.Component.text

/** The quests of the Odyssey content. */
object Quests {
    /** ID of the lighthouse questline, usable before the quest itself is defined. */
    const val LEUCHTFEUER_ID: String = "leuchtfeuer"

    /** The main questline "Das erloschene Leuchtfeuer", given by Brina. */
    val LEUCHTFEUER: QuestDefinition = leuchtfeuer()

    /** The side quest "Das erste Gericht", given by Koch Anselm. */
    val KOCH_FIRST_DISH: QuestDefinition = kochFirstDish()

    /** All quests in the order they are registered. */
    val all: List<QuestDefinition> = listOf(LEUCHTFEUER, KOCH_FIRST_DISH)
}

/**
 * Adds an answer with German texts.
 *
 * @param line the answer line
 * @param hint the hover text of the answer
 * @param action what happens when the answer is picked
 * @param kind how the answer is presented
 */
internal fun DialogNodeBuilder.reply(line: String, hint: String, action: NodeAction, kind: AnswerKind = AnswerKind.NORMAL) {
    answer(text(line), text(hint), action, kind)
}

/**
 * Adds an answer that ends the conversation.
 *
 * @param line the answer line
 */
internal fun DialogNodeBuilder.leave(line: String) {
    answer(text(line), text("Gespräch beenden"), NodeAction.Close, AnswerKind.LEAVE)
}

/**
 * Sets the narrated text of the node.
 *
 * @param line the German text
 */
internal fun DialogNodeBuilder.say(line: String) {
    text = text(line)
}
