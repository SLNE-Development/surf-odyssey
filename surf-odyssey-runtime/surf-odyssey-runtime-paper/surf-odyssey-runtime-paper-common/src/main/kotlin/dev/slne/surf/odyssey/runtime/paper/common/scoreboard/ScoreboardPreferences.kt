package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

import dev.slne.surf.odyssey.core.common.scoreboard.ScoreboardBridge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val saveLocks = ConcurrentHashMap<UUID, Mutex>()

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
     * Flips the setting of [uuid] in the cache without storing it; call [persist] afterwards.
     *
     * @return whether the scoreboard is shown after the flip
     */
    fun toggle(uuid: UUID): Boolean {
        val enabled = disabled.remove(uuid)
        if (!enabled) disabled.add(uuid)
        return enabled
    }

    /**
     * Stores the cached setting of [uuid]. Calls for the same player run one after the other and each stores the
     * value the cache has when its turn comes, so the last call leaves the latest value in the store. A failing
     * store does not change the cache.
     *
     * @return the error of the store, or `null` if the setting was stored
     */
    suspend fun persist(uuid: UUID): Exception? = saveLocks.computeIfAbsent(uuid) { Mutex() }.withLock {
        try {
            store.save(uuid, isEnabled(uuid))
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e
        }
    }

    /** Removes [uuid] from the cache. */
    fun evict(uuid: UUID) {
        disabled.remove(uuid)
        saveLocks.remove(uuid)
    }

    /** Sets the cached setting of [uuid] to [enabled]. */
    private fun cache(uuid: UUID, enabled: Boolean) {
        if (enabled) disabled.remove(uuid) else disabled.add(uuid)
    }
}

/** The preferences of this server. */
internal val scoreboardPreferences = ScoreboardPreferences(BridgeScoreboardStore)
