package dev.slne.surf.odyssey.runtime.paper.hub.common.fishing

import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests how the bite speed bonus shortens the wait of a fishing hook. */
class BiteSpeedTest {
    @Test
    fun `no bonus keeps the wait`() = assertEquals(600, BiteSpeed.scaledWait(600, 0))

    @Test
    fun `a bonus shortens the wait by its percentage`() = assertEquals(450, BiteSpeed.scaledWait(600, 25))

    @Test
    fun `the wait never drops below one tick`() = assertEquals(1, BiteSpeed.scaledWait(600, 100))
}
