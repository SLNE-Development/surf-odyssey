package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests which server [ServerChooser] picks for a traveller. */
class ServerChooserTest {
    @Test
    fun `the running server with the fewest players wins`() = assertEquals(
        "hub-north-02",
        ServerChooser.choose(listOf(ServerCandidate("hub-north-01", true, 12), ServerCandidate("hub-north-02", true, 3), ServerCandidate("hub-north-03", false, 0)))?.name,
    )

    @Test
    fun `ties go to the first name`() = assertEquals(
        "hub-north-01",
        ServerChooser.choose(listOf(ServerCandidate("hub-north-02", true, 1), ServerCandidate("hub-north-01", true, 1)))?.name,
    )

    @Test
    fun `no running server means no target`() =
        assertNull(ServerChooser.choose(listOf(ServerCandidate("hub-north-01", false, 0))))
}
