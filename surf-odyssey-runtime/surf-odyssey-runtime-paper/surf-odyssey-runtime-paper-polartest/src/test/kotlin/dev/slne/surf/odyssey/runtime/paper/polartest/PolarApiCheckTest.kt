package dev.slne.surf.odyssey.runtime.paper.polartest

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks that [PolarApiCheck] tells the current surf-polar-paper API apart from a class path without it. */
class PolarApiCheckTest {
    @Test
    fun `the api on the test class path is the per-plugin api`() {
        assertTrue(PolarApiCheck.isNewApiPresent(PolarApiCheckTest::class.java.classLoader))
    }

    @Test
    fun `a class loader without the api is reported as missing`() {
        assertFalse(PolarApiCheck.isNewApiPresent(object : ClassLoader(null) {}))
    }
}
