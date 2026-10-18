package dev.slne.surf.odyssey.runtime.paper.hub.common.gate

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests how [GateTracker] follows players through locked regions. */
class GateTrackerTest {
    private val player = UUID(0, 1)
    private val damage = GateDamage()

    @Test
    fun `damage starts after the grace period and repeats at the interval`() {
        val tracker = GateTracker()
        assertFalse(tracker.inLockedRegion(player, "mine", damage, 0))
        assertFalse(tracker.inLockedRegion(player, "mine", damage, 4_000))
        assertTrue(tracker.inLockedRegion(player, "mine", damage, 5_000))
        assertFalse(tracker.inLockedRegion(player, "mine", damage, 7_000))
        assertTrue(tracker.inLockedRegion(player, "mine", damage, 8_000))
    }

    @Test
    fun `entering another locked region starts a new grace period`() {
        val tracker = GateTracker()
        tracker.inLockedRegion(player, "mine", damage, 0)
        assertFalse(tracker.inLockedRegion(player, "cave", damage, 6_000))
        assertTrue(tracker.inLockedRegion(player, "cave", damage, 11_000))
    }

    @Test
    fun `leaving and coming back starts a new grace period`() {
        val tracker = GateTracker()
        tracker.inLockedRegion(player, "mine", damage, 0)
        tracker.release(player)
        assertFalse(tracker.inLockedRegion(player, "mine", damage, 6_000))
    }

    @Test
    fun `only a darkened player needs releasing`() {
        val tracker = GateTracker()
        assertFalse(tracker.release(player))
        tracker.inLockedRegion(player, "mine", damage, 0)
        assertTrue(tracker.release(player))
        assertFalse(tracker.release(player))
    }

    @Test
    fun `clearing returns every darkened player`() {
        val tracker = GateTracker()
        val other = UUID(0, 2)
        tracker.inLockedRegion(player, "mine", damage, 0)
        tracker.inLockedRegion(other, "mine", damage, 0)
        assertEquals(setOf(player, other), tracker.clear())
        assertFalse(tracker.release(player))
    }

    @Test
    fun `a world transfer starts once and waits while it runs`() {
        val tracker = GateTracker()
        assertTrue(tracker.tryStartWorldTransfer(player, 0, 5_000))
        assertFalse(tracker.tryStartWorldTransfer(player, 10_000, 5_000))
    }

    @Test
    fun `a failed world transfer is retried after the retry interval`() {
        val tracker = GateTracker()
        tracker.tryStartWorldTransfer(player, 0, 5_000)
        tracker.finishWorldTransfer(player, sent = false)
        assertFalse(tracker.isWorldGatePassed(player))
        assertFalse(tracker.tryStartWorldTransfer(player, 4_999, 5_000))
        assertTrue(tracker.tryStartWorldTransfer(player, 5_000, 5_000))
    }

    @Test
    fun `a successful world transfer ends the world gate check`() {
        val tracker = GateTracker()
        tracker.tryStartWorldTransfer(player, 0, 5_000)
        tracker.finishWorldTransfer(player, sent = true)
        assertTrue(tracker.isWorldGatePassed(player))
    }

    @Test
    fun `forgetting a player resets the world gate check`() {
        val tracker = GateTracker()
        tracker.markWorldGatePassed(player)
        tracker.inLockedRegion(player, "mine", damage, 0)
        assertTrue(tracker.forget(player))
        assertFalse(tracker.isWorldGatePassed(player))
        assertTrue(tracker.tryStartWorldTransfer(player, 0, 5_000))
    }

    @Test
    fun `releasing a player keeps the world gate check`() {
        val tracker = GateTracker()
        tracker.markWorldGatePassed(player)
        tracker.release(player)
        assertTrue(tracker.isWorldGatePassed(player))
    }
}
