package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationOutcome

/**
 * A hint shown to the player in chat when a quest action in a conversation did not go through.
 *
 * @property text the German player text
 */
enum class ConversationHint(val text: String) {
    /** The action was refused, for example because the quest is no longer offered or an objective is open. */
    REFUSED("Das geht gerade nicht."),

    /** The player lacks items the action takes. */
    MISSING_ITEMS("Dir fehlen noch Gegenstände."),

    /** The progression service or the player's progression is not available. */
    UNAVAILABLE("Das ist gerade nicht verfügbar."),
}

/** What a conversation does after a quest action ended. */
sealed interface ConversationStep {
    /**
     * Shows a node of the same quest.
     *
     * @property nodeId the node to show
     */
    data class ShowNode(val nodeId: String) : ConversationStep

    /** Ends the conversation. */
    data object Close : ConversationStep

    /**
     * Ends the conversation and tells the player why the action did not go through.
     *
     * @property hint the hint shown in chat
     */
    data class Hint(val hint: ConversationHint) : ConversationStep

    /** Does nothing, because another action of the player is still running. */
    data object Ignore : ConversationStep
}

/** Maps the outcome of a quest action in a conversation to what the conversation does next. */
object ConversationActions {
    /**
     * Returns what the conversation does after a quest action ended with [outcome].
     *
     * @param outcome the outcome of the action
     * @param then the node shown after a successful action, or `null` to end the conversation
     */
    fun after(outcome: OperationOutcome, then: String?): ConversationStep = when (outcome) {
        OperationOutcome.Success -> then?.let(ConversationStep::ShowNode) ?: ConversationStep.Close
        OperationOutcome.MissingItems -> ConversationStep.Hint(ConversationHint.MISSING_ITEMS)
        OperationOutcome.Unavailable -> ConversationStep.Hint(ConversationHint.UNAVAILABLE)
        is OperationOutcome.Rejected -> ConversationStep.Hint(ConversationHint.REFUSED)
        OperationOutcome.Busy -> ConversationStep.Ignore
    }
}
