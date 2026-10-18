package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests for the tokens that bind answer clicks to the conversation screen a player currently sees. */
class AnswerTokensTest {
    private val alice = UUID.fromString("0b6f3a52-1c1d-4b8e-9d55-0f3a0d7b2c11")
    private val bob = UUID.fromString("5e2b8c70-6a4f-4f31-8a0e-3d9c2b1e7f42")
    private val tokens = AnswerTokens<String>()

    @Test
    fun `the current screen resolves its lines repeatedly`() {
        val token = tokens.show(alice, listOf("a", "b"))

        assertEquals("b", tokens.resolve(alice, token, 1))
        assertEquals("b", tokens.resolve(alice, token, 1))
        assertEquals("a", tokens.resolve(alice, token, 0))
    }

    @Test
    fun `an index outside the lines resolves nothing`() {
        val token = tokens.show(alice, listOf("a"))

        assertNull(tokens.resolve(alice, token, 1))
        assertNull(tokens.resolve(alice, token, -1))
    }

    @Test
    fun `showing a new screen invalidates the earlier token`() {
        val first = tokens.show(alice, listOf("a"))
        val second = tokens.show(alice, listOf("b"))

        assertNotEquals(first, second)
        assertNull(tokens.resolve(alice, first, 0))
        assertEquals("b", tokens.resolve(alice, second, 0))
    }

    @Test
    fun `closing invalidates the token`() {
        val token = tokens.show(alice, listOf("a"))
        tokens.close(alice)

        assertNull(tokens.resolve(alice, token, 0))
        assertFalse(tokens.isCurrent(alice, token))
    }

    @Test
    fun `closing with a stale token keeps the current screen`() {
        val stale = tokens.show(alice, listOf("a"))
        val current = tokens.show(alice, listOf("b"))

        assertFalse(tokens.closeIfCurrent(alice, stale))
        assertEquals("b", tokens.resolve(alice, current, 0))
        assertTrue(tokens.closeIfCurrent(alice, current))
        assertNull(tokens.resolve(alice, current, 0))
    }

    @Test
    fun `tokens are bound to their player`() {
        val token = tokens.show(alice, listOf("a"))
        tokens.show(bob, listOf("b"))

        assertNull(tokens.resolve(bob, token, 0))
        assertTrue(tokens.isCurrent(alice, token))
    }
}
