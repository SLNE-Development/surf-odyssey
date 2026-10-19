package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

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
    private class FakeStore(
        var stored: Boolean = true,
        var loadDelayMillis: Long = 0,
        var failLoad: Boolean = false,
        var failSave: Boolean = false,
    ) : ScoreboardStore {
        val saves = mutableListOf<Boolean>()

        override suspend fun load(uuid: UUID): Boolean {
            if (loadDelayMillis > 0) delay(loadDelayMillis)
            if (failLoad) error("down")
            return stored
        }

        override suspend fun save(uuid: UUID, enabled: Boolean) {
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
    fun `toggle flips and stores the setting`() = runBlocking {
        val store = FakeStore()
        val preferences = ScoreboardPreferences(store)

        val first = preferences.toggle(uuid)
        val second = preferences.toggle(uuid)

        assertFalse(first.enabled)
        assertNull(first.saveError)
        assertTrue(second.enabled)
        assertEquals(listOf(false, true), store.saves)
    }

    @Test
    fun `a failing save keeps the new value and reports the error`() = runBlocking {
        val preferences = ScoreboardPreferences(FakeStore(failSave = true))

        val result = preferences.toggle(uuid)

        assertFalse(result.enabled)
        assertNotNull(result.saveError)
        assertFalse(preferences.isEnabled(uuid))
    }

    @Test
    fun `evict forgets the setting`() = runBlocking {
        val preferences = ScoreboardPreferences(FakeStore(stored = false))
        preferences.load(uuid)

        preferences.evict(uuid)

        assertTrue(preferences.isEnabled(uuid))
    }
}
