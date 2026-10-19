package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/** Tests for the cached scoreboard setting of the online players. */
class ScoreboardPreferencesTest {
    /** A store in memory whose load and save can be slowed down or made to fail. */
    private class FakeStore(
        var stored: Boolean = true,
        var loadDelayMillis: Long = 0,
        var failLoad: Boolean = false,
        var failSave: Boolean = false,
        val saveDelaysMillis: MutableList<Long> = mutableListOf(),
    ) : ScoreboardStore {
        val saves = mutableListOf<Boolean>()

        override suspend fun load(uuid: UUID): Boolean {
            if (loadDelayMillis > 0) delay(loadDelayMillis)
            if (failLoad) error("down")
            return stored
        }

        override suspend fun save(uuid: UUID, enabled: Boolean) {
            val delayMillis = saveDelaysMillis.removeFirstOrNull() ?: 0L
            if (delayMillis > 0) delay(delayMillis)
            if (failSave) error("down")
            saves += enabled
        }
    }

    private val uuid = UUID.randomUUID()

    @Test
    fun `a player that was never loaded sees the scoreboard`() {
        assertTrue(ScoreboardPreferences(FakeStore()).isEnabled(uuid))
    }

    @Test
    fun `loads a disabled setting`() = runBlocking {
        val preferences = ScoreboardPreferences(FakeStore(stored = false))

        preferences.load(uuid)

        assertFalse(preferences.isEnabled(uuid))
    }

    @Test
    fun `a setting that loads too slowly leaves the scoreboard on`() = runBlocking {
        val preferences = ScoreboardPreferences(
            FakeStore(stored = false, loadDelayMillis = 5_000), loadTimeout = 50.milliseconds
        )

        preferences.load(uuid)

        assertTrue(preferences.isEnabled(uuid))
    }

    @Test
    fun `a failing load is reported and leaves the scoreboard on`() = runBlocking {
        val preferences = ScoreboardPreferences(FakeStore(failLoad = true))

        assertFailsWith<IllegalStateException> { preferences.load(uuid) }
        assertTrue(preferences.isEnabled(uuid))
    }

    @Test
    fun `toggle flips the cached setting`() {
        val preferences = ScoreboardPreferences(FakeStore())

        assertFalse(preferences.toggle(uuid))
        assertFalse(preferences.isEnabled(uuid))
        assertTrue(preferences.toggle(uuid))
        assertTrue(preferences.isEnabled(uuid))
    }

    @Test
    fun `persist stores the current setting`() = runBlocking {
        val store = FakeStore()
        val preferences = ScoreboardPreferences(store)
        preferences.toggle(uuid)

        assertNull(preferences.persist(uuid))

        assertEquals(listOf(false), store.saves)
    }

    @Test
    fun `a failing save keeps the new value and returns the error`() = runBlocking {
        val preferences = ScoreboardPreferences(FakeStore(failSave = true))
        preferences.toggle(uuid)

        assertNotNull(preferences.persist(uuid))
        assertFalse(preferences.isEnabled(uuid))
    }

    @Test
    fun `quick toggles end with the last value in the store`() = runBlocking {
        val store = FakeStore(saveDelaysMillis = mutableListOf(100L, 0L))
        val preferences = ScoreboardPreferences(store)

        val first = async { preferences.toggle(uuid); preferences.persist(uuid) }
        val second = async { preferences.toggle(uuid); preferences.persist(uuid) }
        first.await()
        second.await()

        assertEquals(preferences.isEnabled(uuid), store.saves.last())
    }

    @Test
    fun `evict forgets the setting`() = runBlocking {
        val preferences = ScoreboardPreferences(FakeStore(stored = false))
        preferences.load(uuid)

        preferences.evict(uuid)

        assertTrue(preferences.isEnabled(uuid))
    }
}
