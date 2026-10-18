package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.NodeAction

/** Which part of a quest a shown node belongs to. */
sealed interface NodePhase {
    /** The node offers the quest, which the player has not accepted. */
    data object Offered : NodePhase

    /**
     * The node belongs to a step of the active quest.
     *
     * @property index the index of the step
     */
    data class Step(val index: Int) : NodePhase

    /** The node is shown after the quest was completed or is otherwise no longer open. */
    data object Done : NodePhase
}

/** The rules a clicked conversation answer has to pass before it runs. */
object ConversationChecks {
    /** The largest distance in blocks between a player and the NPC they talk to. */
    const val MAX_DISTANCE: Double = 8.0

    /**
     * Returns whether a player at [distanceSquared] from the NPC is close enough to talk to it.
     *
     * @param distanceSquared the squared distance in blocks
     */
    fun inReach(distanceSquared: Double): Boolean = distanceSquared <= MAX_DISTANCE * MAX_DISTANCE

    /**
     * Returns whether the quest is still where [phase] says.
     *
     * @param phase the phase of the shown node
     * @param offered whether the quest is offered to the player now
     * @param currentStep the current step of the active quest, or `null` if the quest is not active
     */
    fun phaseHolds(phase: NodePhase, offered: Boolean, currentStep: Int?): Boolean = when (phase) {
        NodePhase.Offered -> offered
        is NodePhase.Step -> currentStep == phase.index
        NodePhase.Done -> true
    }

    /**
     * Returns whether [action] may run in a node of [phase]: after the quest only actions that do not change it.
     *
     * @param phase the phase of the shown node
     * @param action the picked action
     */
    fun allows(phase: NodePhase, action: NodeAction): Boolean = phase != NodePhase.Done || when (action) {
        is NodeAction.Goto, NodeAction.Close, NodeAction.OpenTrade, NodeAction.OpenStation -> true
        is NodeAction.AcceptQuest, is NodeAction.CompleteTalk, is NodeAction.DeliverItems, is NodeAction.TurnIn -> false
    }
}
