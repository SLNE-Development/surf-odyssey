package dev.slne.surf.odyssey.runtime.paper.hub.common.gate

import org.bukkit.event.block.Action
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests which denied interactions remind the player of a locked region. */
class GateListenerTest {
    @Test
    fun `clicks on blocks remind the player`() {
        assertTrue(GateListener.remindsOn(Action.LEFT_CLICK_BLOCK))
        assertTrue(GateListener.remindsOn(Action.RIGHT_CLICK_BLOCK))
    }

    @Test
    fun `stepping on blocks and clicks into the air do not remind the player`() {
        assertFalse(GateListener.remindsOn(Action.PHYSICAL))
        assertFalse(GateListener.remindsOn(Action.LEFT_CLICK_AIR))
        assertFalse(GateListener.remindsOn(Action.RIGHT_CLICK_AIR))
    }
}
