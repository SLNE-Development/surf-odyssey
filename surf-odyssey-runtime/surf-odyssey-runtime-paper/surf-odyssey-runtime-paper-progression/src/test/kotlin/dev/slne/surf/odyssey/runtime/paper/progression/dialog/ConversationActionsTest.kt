package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for what a conversation shows after a quest action ended. */
class ConversationActionsTest {
    @Test
    fun `success shows the then node`() {
        assertEquals(ConversationStep.ShowNode("brina_accept"), ConversationActions.after(OperationOutcome.Success, "brina_accept"))
    }

    @Test
    fun `success without then node closes the conversation`() {
        assertEquals(ConversationStep.Close, ConversationActions.after(OperationOutcome.Success, null))
    }

    @Test
    fun `missing items show the missing items hint`() {
        assertEquals(
            ConversationStep.Hint(ConversationHint.MISSING_ITEMS),
            ConversationActions.after(OperationOutcome.MissingItems, "brina_thanks"),
        )
    }

    @Test
    fun `an unreachable service shows the unavailable hint`() {
        assertEquals(
            ConversationStep.Hint(ConversationHint.UNAVAILABLE),
            ConversationActions.after(OperationOutcome.Unavailable, "brina_thanks"),
        )
    }

    @Test
    fun `an operation that was not started shows the unavailable hint`() {
        assertEquals(
            ConversationStep.Hint(ConversationHint.UNAVAILABLE),
            ConversationActions.after(OperationOutcome.NotStarted, "brina_thanks"),
        )
    }

    @Test
    fun `a refusal shows the refused hint`() {
        assertEquals(
            ConversationStep.Hint(ConversationHint.REFUSED),
            ConversationActions.after(OperationOutcome.Rejected("quest lost_net is not offered"), "brina_thanks"),
        )
    }

    @Test
    fun `a busy player is ignored`() {
        assertEquals(ConversationStep.Ignore, ConversationActions.after(OperationOutcome.Busy, "brina_thanks"))
    }

    @Test
    fun `hints carry the player texts`() {
        assertEquals("Das geht gerade nicht.", ConversationHint.REFUSED.text)
        assertEquals("Dir fehlen noch Gegenstände.", ConversationHint.MISSING_ITEMS.text)
        assertEquals("Das ist gerade nicht verfügbar.", ConversationHint.UNAVAILABLE.text)
    }
}
