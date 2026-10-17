package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HubTravelTest {
    /** Records what [HubTravel.travel] asked of it and answers transfers with [transferWorks]. */
    private class RecordingGateway(private val transferWorks: Boolean = true) : TravelGateway {
        /** Every call in order, e.g. `transfer:hub-north-01`. */
        val calls = mutableListOf<String>()

        /** Records the transfer and answers with [transferWorks]. */
        override suspend fun transfer(targetServer: String): Boolean {
            calls += "transfer:$targetServer"
            return transferWorks
        }

        /** Records the message. */
        override fun tell(message: String) {
            calls += "tell:$message"
        }
    }

    @Test
    fun `no running instance means a message and no transfer`() = runBlocking {
        val gateway = RecordingGateway()
        val sent = HubTravel.travel(
            listOf(ServerCandidate("hub-north-01", false, 0), ServerCandidate("hub-north-02", false, 3)),
            gateway,
        )
        assertFalse(sent)
        assertEquals(listOf("tell:${HubTravel.UNREACHABLE}"), gateway.calls)
    }

    @Test
    fun `no instance at all means a message and no transfer`() = runBlocking {
        val gateway = RecordingGateway()
        assertFalse(HubTravel.travel(emptyList(), gateway))
        assertEquals(listOf("tell:${HubTravel.UNREACHABLE}"), gateway.calls)
    }

    @Test
    fun `the player is sent to the least crowded running instance`() = runBlocking {
        val gateway = RecordingGateway()
        val sent = HubTravel.travel(
            listOf(ServerCandidate("hub-north-01", true, 5), ServerCandidate("hub-north-02", true, 1)),
            gateway,
        )
        assertTrue(sent)
        assertEquals(listOf("transfer:hub-north-02"), gateway.calls)
    }

    @Test
    fun `a failed transfer tells the player`() = runBlocking {
        val gateway = RecordingGateway(transferWorks = false)
        val sent = HubTravel.travel(listOf(ServerCandidate("hub-north-01", true, 0)), gateway)
        assertFalse(sent)
        assertEquals(listOf("transfer:hub-north-01", "tell:${HubTravel.FAILED}"), gateway.calls)
    }
}
