package dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog

import dev.slne.surf.odyssey.runtime.paper.progression.definition.ProgressionDsl
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import net.kyori.adventure.text.Component

/** How an answer is presented in a conversation. */
enum class AnswerKind {
    /** A regular answer. */
    NORMAL,

    /** An answer that accepts something, such as a quest. */
    ACCEPT,

    /** An answer that leaves the conversation or declines. */
    LEAVE,
}

/** What happens when a player picks an answer. */
sealed interface NodeAction {
    /**
     * Shows another node of the same quest.
     *
     * @property nodeId the node shown next
     */
    data class Goto(val nodeId: String) : NodeAction

    /**
     * Accepts the quest of the node.
     *
     * @property then node shown afterwards, or `null` to end the conversation
     */
    data class AcceptQuest(val then: String?) : NodeAction

    /**
     * Fulfils the `TalkTo` objective of the current step.
     *
     * @property then node shown afterwards, or `null` to end the conversation
     */
    data class CompleteTalk(val then: String?) : NodeAction

    /**
     * Hands in the items of every `Deliver` objective of the current step that targets this NPC.
     *
     * @property then node shown afterwards, or `null` to end the conversation
     */
    data class DeliverItems(val then: String?) : NodeAction

    /**
     * Completes the current step, or the quest after its last step, when all objectives are met.
     *
     * @property then node shown afterwards, or `null` to end the conversation
     */
    data class TurnIn(val then: String?) : NodeAction

    /** Opens the trade menu of the NPC. */
    data object OpenTrade : NodeAction

    /** Opens the station menu of the NPC. */
    data object OpenStation : NodeAction

    /** Ends the conversation. */
    data object Close : NodeAction
}

/**
 * One answer a player can pick in a conversation node.
 *
 * @property text the answer line
 * @property tooltip hover text of the answer line
 * @property kind how the answer is presented
 * @property visibleWhen requirement for the answer to be shown, or `null` if it is always shown
 * @property action what happens when the answer is picked
 */
class Answer(
    val text: Component,
    val tooltip: Component,
    val kind: AnswerKind,
    val visibleWhen: Requirement?,
    val action: NodeAction,
)

/**
 * One screen of a conversation: the speaking NPC, its mood, the narrated text and the answers.
 *
 * @property id node ID, unique within its quest
 * @property npc the speaking NPC
 * @property mood portrait mood of the NPC
 * @property meta optional meta line shown above the text
 * @property text the narrated text
 * @property answers answers the player can pick
 */
class DialogNode(
    val id: String,
    val npc: NpcDefinition,
    val mood: String,
    val meta: Component?,
    val text: Component,
    val answers: List<Answer>,
) {
    /** Returns [id] for log and validation messages. */
    override fun toString(): String = "DialogNode($id)"
}

/**
 * Builds the values of a [DialogNode].
 *
 * @property id the node ID
 * @property npc the speaking NPC
 */
@ProgressionDsl
class DialogNodeBuilder(private val id: String, private val npc: NpcDefinition) {
    /** Portrait mood of the NPC. */
    var mood: String = NpcDefinition.NEUTRAL_MOOD

    /** Optional meta line shown above the text. */
    var meta: Component? = null

    /** The narrated text; required. */
    var text: Component? = null

    /** Answers added so far. */
    private val answers = mutableListOf<Answer>()

    /**
     * Adds an answer.
     *
     * @param text the answer line
     * @param tooltip hover text of the answer line
     * @param action what happens when the answer is picked
     * @param kind how the answer is presented
     * @param visibleWhen requirement for the answer to be shown, or `null` if it is always shown
     */
    fun answer(
        text: Component,
        tooltip: Component,
        action: NodeAction,
        kind: AnswerKind = AnswerKind.NORMAL,
        visibleWhen: Requirement? = null,
    ) {
        answers += Answer(text, tooltip, kind, visibleWhen, action)
    }

    /**
     * Creates the node.
     *
     * @throws IllegalStateException if [text] is missing
     */
    fun build(): DialogNode = DialogNode(
        id = id,
        npc = npc,
        mood = mood,
        meta = meta,
        text = checkNotNull(text) { "node $id needs a text" },
        answers = answers.toList(),
    )
}
