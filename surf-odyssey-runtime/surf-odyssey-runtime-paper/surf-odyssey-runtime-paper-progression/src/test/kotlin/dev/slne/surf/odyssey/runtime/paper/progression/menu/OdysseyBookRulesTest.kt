package dev.slne.surf.odyssey.runtime.paper.progression.menu

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for which inventory moves the Odyssey book blocks. */
class OdysseyBookRulesTest {
    @Test
    fun `dropping the book is blocked`() {
        assertTrue(BookRules.isProtectedMove(BookMove.DROP, slot = null, isBook = true))
        assertFalse(BookRules.isProtectedMove(BookMove.DROP, slot = null, isBook = false))
    }

    @Test
    fun `shift clicking the book is blocked`() {
        assertTrue(BookRules.isProtectedMove(BookMove.SHIFT_CLICK, slot = 8, isBook = true))
        assertTrue(BookRules.isProtectedMove(BookMove.SHIFT_CLICK, slot = 20, isBook = true))
        assertFalse(BookRules.isProtectedMove(BookMove.SHIFT_CLICK, slot = 20, isBook = false))
    }

    @Test
    fun `clicking the book slot is blocked even without the book in it`() {
        assertTrue(BookRules.isProtectedMove(BookMove.CLICK, slot = BookRules.BOOK_SLOT, isBook = false))
        assertFalse(BookRules.isProtectedMove(BookMove.CLICK, slot = 3, isBook = false))
        assertFalse(BookRules.isProtectedMove(BookMove.CLICK, slot = null, isBook = false))
    }

    @Test
    fun `a number key swap with the book slot is blocked`() {
        assertTrue(BookRules.isProtectedMove(BookMove.NUMBER_KEY, slot = BookRules.BOOK_SLOT, isBook = false))
        assertTrue(BookRules.isProtectedMove(BookMove.NUMBER_KEY, slot = 2, isBook = true))
        assertFalse(BookRules.isProtectedMove(BookMove.NUMBER_KEY, slot = 2, isBook = false))
    }

    @Test
    fun `moving the book into the off hand is blocked`() {
        assertTrue(BookRules.isProtectedMove(BookMove.OFFHAND_SWAP, slot = null, isBook = true))
        assertFalse(BookRules.isProtectedMove(BookMove.OFFHAND_SWAP, slot = null, isBook = false))
    }

    @Test
    fun `dragging over the book slot is blocked`() {
        assertTrue(BookRules.isProtectedMove(BookMove.DRAG, slot = BookRules.BOOK_SLOT, isBook = false))
        assertFalse(BookRules.isProtectedMove(BookMove.DRAG, slot = 12, isBook = false))
    }
}
