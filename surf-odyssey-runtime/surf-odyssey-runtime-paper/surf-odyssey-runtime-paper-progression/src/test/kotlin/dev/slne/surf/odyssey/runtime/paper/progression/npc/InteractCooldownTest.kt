package dev.slne.surf.odyssey.runtime.paper.progression.npc

import java.util.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/** Tests for the per-player cooldown between two NPC interactions. */
class InteractCooldownTest {
    private val alice = UUID.fromString("0b6f3a52-1c1d-4b8e-9d55-0f3a0d7b2c11")
    private val bob = UUID.fromString("5e2b8c70-6a4f-4f31-8a0e-3d9c2b1e7f42")
    private var nanos = 0L
    private val cooldown = InteractCooldown(500.milliseconds) { nanos }

    @Test
    fun `a second interaction within the window is refused`() {
        assertTrue(cooldown.tryAcquire(alice))
        nanos += 499.milliseconds.inWholeNanoseconds
        assertFalse(cooldown.tryAcquire(alice))
    }

    @Test
    fun `an interaction after the window is allowed`() {
        assertTrue(cooldown.tryAcquire(alice))
        nanos += 500.milliseconds.inWholeNanoseconds
        assertTrue(cooldown.tryAcquire(alice))
    }

    @Test
    fun `players have separate windows`() {
        assertTrue(cooldown.tryAcquire(alice))
        assertTrue(cooldown.tryAcquire(bob))
    }

    @Test
    fun `a forgotten player starts without a window`() {
        assertTrue(cooldown.tryAcquire(alice))
        cooldown.forget(alice)
        assertTrue(cooldown.tryAcquire(alice))
    }
}
