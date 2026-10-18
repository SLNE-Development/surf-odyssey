package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.NodeAction
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for the checks a clicked conversation answer has to pass. */
class ConversationChecksTest {
    @Test
    fun `players within 8 blocks are in reach`() {
        assertTrue(ConversationChecks.inReach(64.0))
        assertFalse(ConversationChecks.inReach(64.01))
    }

    @Test
    fun `an offer node holds only while the quest is offered`() {
        assertTrue(ConversationChecks.phaseHolds(NodePhase.Offered, offered = true, currentStep = null))
        assertFalse(ConversationChecks.phaseHolds(NodePhase.Offered, offered = false, currentStep = 0))
    }

    @Test
    fun `a step node holds only while the quest is on that step`() {
        assertTrue(ConversationChecks.phaseHolds(NodePhase.Step(1), offered = false, currentStep = 1))
        assertFalse(ConversationChecks.phaseHolds(NodePhase.Step(1), offered = false, currentStep = 2))
        assertFalse(ConversationChecks.phaseHolds(NodePhase.Step(1), offered = true, currentStep = null))
    }

    @Test
    fun `a node after the quest allows only actions without quest change`() {
        assertTrue(ConversationChecks.phaseHolds(NodePhase.Done, offered = false, currentStep = null))
        assertTrue(ConversationChecks.allows(NodePhase.Done, NodeAction.Goto("bye")))
        assertTrue(ConversationChecks.allows(NodePhase.Done, NodeAction.Close))
        assertTrue(ConversationChecks.allows(NodePhase.Done, NodeAction.OpenTrade))
        assertFalse(ConversationChecks.allows(NodePhase.Done, NodeAction.AcceptQuest(null)))
        assertFalse(ConversationChecks.allows(NodePhase.Done, NodeAction.TurnIn(null)))
        assertTrue(ConversationChecks.allows(NodePhase.Step(0), NodeAction.TurnIn(null)))
    }
}
