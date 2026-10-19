package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

import dev.slne.surf.odyssey.core.common.scoreboard.ScoreboardBridge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Persistent storage of the scoreboard setting. */
interface ScoreboardStore {
    /** Returns whether [uuid] sees the scoreboard. */
    suspend fun load(uuid: UUID): Boolean

    /** Stores whether [uuid] sees the scoreboard. */
    suspend fun save(uuid: UUID, enabled: Boolean)
}

/** The [ScoreboardStore] that talks to the microservice. */
internal object BridgeScoreboardStore : ScoreboardStore {
    override suspend fun load(uuid: UUID): Boolean = ScoreboardBridge.isEnabled(uuid)

    override suspend fun save(uuid: UUID, enabled: Boolean) = ScoreboardBridge.setEnabled(uuid, enabled)
}

/**
 * The outcome of [ScoreboardPreferences.toggle].
 *
 * @property enabled whether the scoreboard is shown after the toggle
 * @property saveError why the new value could not be stored, or `null` if it was stored
 */
data class ScoreboardToggle(val enabled: Boolean, val saveError: Exception?)

/**
 * Caches, for every online player, whether they see the scoreboard.
 *
 * @param store where the setting is stored
 * @param loadTimeout how long [load] waits for [store] before it leaves the scoreboard on
 */
class ScoreboardPreferences(
    private val store: ScoreboardStore,
    private val loadTimeout: Duration = 2.seconds,
) {
    private val disabled = ConcurrentHashMap.newKeySet<UUID>()

    /** Whether [uuid] sees the scoreboard; players that are not cached see it. */
    fun isEnabled(uuid: UUID): Boolean = uuid !in disabled

    /**
     * Loads the stored setting of [uuid] into the cache. If [store] answers later than the load timeout, the
     * scoreboard stays on.
     *
     * @throws Exception if [store] fails; the scoreboard stays on
     */
    suspend fun load(uuid: UUID) {
        val enabled = withTimeoutOrNull(loadTimeout) { store.load(uuid) } ?: return
        cache(uuid, enabled)
    }

    /**
     * Flips the setting of [uuid] in the cache and then stores it. A failing store does not undo the flip.
     *
     * @return the new value and the error of the store, if any
     */
    suspend fun toggle(uuid: UUID): ScoreboardToggle {
        val enabled = disabled.remove(uuid)
        if (!enabled) disabled.add(uuid)

        return try {
            store.save(uuid, enabled)
            ScoreboardToggle(enabled, null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ScoreboardToggle(enabled, e)
        }
    }

    /** Removes [uuid] from the cache. */
    fun evict(uuid: UUID) {
        disabled.remove(uuid)
    }

    private fun cache(uuid: UUID, enabled: Boolean) {
        if (enabled) disabled.remove(uuid) else disabled.add(uuid)
    }
}

/** The preferences of this server. */
internal val scoreboardPreferences = ScoreboardPreferences(BridgeScoreboardStore)
