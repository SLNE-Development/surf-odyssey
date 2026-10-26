package dev.slne.surf.odyssey.runtime.paper.polartest.session

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Checks the message a step gets when it needs a test world that was never set up. */
class RequireTestWorldTest {
    @Test
    fun `a missing world names the setup step`() {
        val error = assertFailsWith<IllegalStateException> { requireTestWorld(null) }
        assertEquals("Keine Testwelt angelegt, zuerst /polartest setup", error.message)
    }

    @Test
    fun `an existing world is returned`() {
        val uuid = UUID.randomUUID()
        assertEquals(uuid, requireTestWorld(uuid))
    }
}
