package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests that [PortalCooldown] lets each player use portals once per cooldown. */
class PortalCooldownTest {
    @Test
    fun `a player can use a portal once per cooldown`() {
        val cooldown = PortalCooldown(3_000)
        val id = UUID.randomUUID()
        assertTrue(cooldown.tryUse(id, now = 0))
        assertFalse(cooldown.tryUse(id, now = 2_999))
        assertTrue(cooldown.tryUse(id, now = 3_000))
    }

    @Test
    fun `running through a portal block by block allows only one transfer`() {
        val cooldown = PortalCooldown(3_000)
        val id = UUID.randomUUID()
        val allowed = (0L..1_000L step 50).count { cooldown.tryUse(id, now = it) }
        assertEquals(1, allowed)
    }

    @Test
    fun `the cooldown of one player does not block another`() {
        val cooldown = PortalCooldown(3_000)
        assertTrue(cooldown.tryUse(UUID.randomUUID(), now = 0))
        assertTrue(cooldown.tryUse(UUID.randomUUID(), now = 0))
    }

    @Test
    fun `a cleared player can use a portal again at once`() {
        val cooldown = PortalCooldown(3_000)
        val id = UUID.randomUUID()
        assertTrue(cooldown.tryUse(id, now = 0))
        cooldown.clear(id)
        assertTrue(cooldown.tryUse(id, now = 1))
    }
}
