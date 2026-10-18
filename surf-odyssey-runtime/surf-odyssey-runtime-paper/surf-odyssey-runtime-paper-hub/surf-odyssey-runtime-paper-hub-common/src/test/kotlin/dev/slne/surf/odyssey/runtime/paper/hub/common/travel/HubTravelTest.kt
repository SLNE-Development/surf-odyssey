package dev.slne.surf.odyssey.runtime.paper.hub.common.travel

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests how [HubTravel.travel] chooses a target, announces the arrival and handles failures. */
class HubTravelTest {
    /**
     * Records what [HubTravel.travel] asked of it, offers [servers], answers transfers with [transferWorks]
     * and throws at the step named by [throwAt].
     */
    private class RecordingGateway(
        private val servers: List<ServerCandidate> = emptyList(),
        private val transferWorks: Boolean = true,
        private val throwAt: String? = null,
    ) : TravelGateway {
        /** Every call in order, e.g. `announce:hub-north-01`. */
        val calls = mutableListOf<String>()

        /** Throws if [step] is the step to fail at. */
        private fun failAt(step: String) {
            if (throwAt == step) throw IllegalStateException("$step failed")
        }

        /** Records the lookup and returns [servers]. */
        override suspend fun candidates(): Collection<ServerCandidate> {
            calls += "candidates"
            failAt("candidates")
            return servers
        }

        /** Records the announcement. */
        override suspend fun announceArrival(targetServer: String) {
            calls += "announce:$targetServer"
            failAt("announce")
        }

        /** Records the transfer and answers with [transferWorks]. */
        override suspend fun transfer(targetServer: String): Boolean {
            calls += "transfer:$targetServer"
            failAt("transfer")
            return transferWorks
        }

        /** Records the withdrawal. */
        override suspend fun withdrawArrival() {
            calls += "withdraw"
            failAt("withdraw")
        }

        /** Records the message. */
        override fun tell(message: String) {
            calls += "tell:$message"
        }

        /** Records the logged error. */
        override fun logFailure(exception: Exception) {
            calls += "log:${exception.message}"
        }
    }

    @Test
    fun `no running instance means a message and no transfer`() = runBlocking {
        val gateway = RecordingGateway(listOf(ServerCandidate("hub-north-01", false, 0), ServerCandidate("hub-north-02", false, 3)))
        assertFalse(HubTravel.travel(gateway))
        assertEquals(listOf("candidates", "tell:${HubTravel.UNREACHABLE}"), gateway.calls)
    }

    @Test
    fun `no instance at all means a message and no transfer`() = runBlocking {
        val gateway = RecordingGateway()
        assertFalse(HubTravel.travel(gateway))
        assertEquals(listOf("candidates", "tell:${HubTravel.UNREACHABLE}"), gateway.calls)
    }

    @Test
    fun `the arrival is announced before the player is sent to the least crowded instance`() = runBlocking {
        val gateway = RecordingGateway(listOf(ServerCandidate("hub-north-01", true, 5), ServerCandidate("hub-north-02", true, 1)))
        assertTrue(HubTravel.travel(gateway))
        assertEquals(listOf("candidates", "announce:hub-north-02", "transfer:hub-north-02"), gateway.calls)
    }

    @Test
    fun `a failed transfer withdraws the arrival and tells the player`() = runBlocking {
        val gateway = RecordingGateway(listOf(ServerCandidate("hub-north-01", true, 0)), transferWorks = false)
        assertFalse(HubTravel.travel(gateway))
        assertEquals(
            listOf("candidates", "announce:hub-north-01", "transfer:hub-north-01", "withdraw", "tell:${HubTravel.FAILED}"),
            gateway.calls,
        )
    }

    @Test
    fun `an error while looking up the servers is logged and told`() = runBlocking {
        val gateway = RecordingGateway(listOf(ServerCandidate("hub-north-01", true, 0)), throwAt = "candidates")
        assertFalse(HubTravel.travel(gateway))
        assertEquals(listOf("candidates", "log:candidates failed", "tell:${HubTravel.FAILED}"), gateway.calls)
    }

    @Test
    fun `an error while announcing the arrival withdraws it, is logged and told`() = runBlocking {
        val gateway = RecordingGateway(listOf(ServerCandidate("hub-north-01", true, 0)), throwAt = "announce")
        assertFalse(HubTravel.travel(gateway))
        assertEquals(
            listOf("candidates", "announce:hub-north-01", "withdraw", "log:announce failed", "tell:${HubTravel.FAILED}"),
            gateway.calls,
        )
    }

    @Test
    fun `an error during the transfer withdraws the arrival, is logged and told`() = runBlocking {
        val gateway = RecordingGateway(listOf(ServerCandidate("hub-north-01", true, 0)), throwAt = "transfer")
        assertFalse(HubTravel.travel(gateway))
        assertEquals(
            listOf(
                "candidates", "announce:hub-north-01", "transfer:hub-north-01", "withdraw",
                "log:transfer failed", "tell:${HubTravel.FAILED}",
            ),
            gateway.calls,
        )
    }

    @Test
    fun `an error while withdrawing after a failed transfer is still logged and told`() = runBlocking {
        val gateway = RecordingGateway(listOf(ServerCandidate("hub-north-01", true, 0)), transferWorks = false, throwAt = "withdraw")
        assertFalse(HubTravel.travel(gateway))
        assertEquals(
            listOf(
                "candidates", "announce:hub-north-01", "transfer:hub-north-01", "withdraw",
                "log:withdraw failed", "tell:${HubTravel.FAILED}",
            ),
            gateway.calls,
        )
    }
}
