package dev.slne.surf.odyssey.runtime.paper.progression.state

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.odyssey.core.common.progression.CounterDeltaDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionBridge
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.time.Clock
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Loads, caches and saves the progression of the players on this server.
 *
 * When a player joins, their state is loaded, retrying with a growing delay until it succeeds or the player leaves,
 * and loaded once more [RELOAD_AFTER_JOIN] after the join so that deltas another server sent late are included.
 * Counter deltas are applied to the cache at once and buffered; the buffers are sent every [FLUSH_INTERVAL] and
 * when the player quits. Every failed call to the progression service marks it unavailable; while it is, the service
 * is pinged every [PING_INTERVAL] until it answers.
 *
 * @param plugin the plugin that owns the coroutines and the log
 * @param clock the clock the cached states use
 * @param bridge the progression service
 * @param onLoaded called after the first successful load of a player who is still online
 */
class ProgressionStore(
    private val plugin: Plugin,
    private val clock: Clock,
    private val bridge: ProgressionBridge = ProgressionBridge,
    private val onLoaded: (Player, PlayerProgression) -> Unit = { _, _ -> },
) : Listener {
    /** The cached progression of every loaded player. */
    private val players = ConcurrentHashMap<UUID, PlayerProgression>()

    /** The counter deltas of each player that were not sent yet, kept until they are sent even after a quit. */
    private val buffers = ConcurrentHashMap<UUID, CounterBuffer>()

    /** Serialises the flushes and reloads of each player. */
    private val locks = ConcurrentHashMap<UUID, Mutex>()

    /** The running load of each joining player. */
    private val loads = ConcurrentHashMap<UUID, Job>()

    /** The background loops started by [start]. */
    private val loops = mutableListOf<Job>()

    /**
     * Returns the cached progression of [player].
     *
     * @return the progression, or `null` while the player's state is not loaded
     */
    operator fun get(player: UUID): PlayerProgression? = players[player]

    /** Returns whether the state of [player] is loaded and the progression service is reachable. */
    fun isAvailableFor(player: UUID): Boolean = Progression.isAvailable && players.containsKey(player)

    /**
     * Adds a counter delta of [player] to the cache and to the buffer that is sent with the next flush.
     *
     * @param player the player who made progress
     * @param delta the amount to add to the counter
     */
    fun addCounter(player: UUID, delta: CounterDeltaDto) {
        bufferOf(player).add(delta)
        players[player]?.addCounter(delta)
    }

    /**
     * Applies the changes of a committed operation to the cache of [player], if the player is loaded.
     *
     * @param player the player the operation belongs to
     * @param changes the committed changes
     */
    fun apply(player: UUID, changes: List<StateChange>) {
        players[player]?.apply(changes)
    }

    /**
     * Abandons the active quest [questId] of [player] at the progression service. On success the quest's buffered
     * deltas are dropped and the cache marks it abandoned.
     *
     * @param player the player
     * @param questId the id of the quest definition
     * @return `true` if the quest was active and is now abandoned, `false` if it was not active or the call failed
     */
    suspend fun abandon(player: UUID, questId: String): Boolean = lockOf(player).withLock {
        try {
            val abandoned = bridge.abandonQuest(player, questId)
            Progression.markAvailable()
            if (abandoned) {
                bufferOf(player).discard(questId)
                players[player]?.abandon(questId)
            }
            abandoned
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail("abandon the quest $questId of $player", e)
            false
        }
    }

    /**
     * Sets the quest [player] tracks at the progression service and in the cache.
     *
     * @param player the player
     * @param questId the id of the quest to track, or `null` for none
     * @return `false` if the call failed and nothing changed
     */
    suspend fun setTrackedQuest(player: UUID, questId: String?): Boolean {
        try {
            bridge.setTrackedQuest(player, questId)
            Progression.markAvailable()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail("set the tracked quest of $player", e)
            return false
        }
        players[player]?.setTrackedQuest(questId)
        return true
    }

    /**
     * Sets the title [player] shows at the progression service and in the cache.
     *
     * @param player the player
     * @param titleId the id of the title to show, or `null` for none
     * @return `true` if the title is set, `false` if the player has not earned it or the call failed
     */
    suspend fun setActiveTitle(player: UUID, titleId: String?): Boolean {
        val set = try {
            bridge.setActiveTitle(player, titleId).also { Progression.markAvailable() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail("set the active title of $player", e)
            return false
        }
        if (set) players[player]?.setActiveTitle(titleId)
        return set
    }

    /**
     * Runs [call] against the progression service under the lock of [player], then loads the state of [player] again.
     * The buffered deltas of [discardedQuest] are dropped first, so that they cannot bring a reset quest back.
     *
     * @param player the player whose stored progression [call] changes
     * @param discardedQuest the id of a quest whose buffered deltas are dropped, or `null` for none
     * @param call the change to send to the service
     * @return `false` if the call failed and nothing was reloaded
     */
    suspend fun change(player: UUID, discardedQuest: String?, call: suspend (ProgressionBridge) -> Unit): Boolean {
        try {
            lockOf(player).withLock {
                call(bridge)
                Progression.markAvailable()
                if (discardedQuest != null) bufferOf(player).discard(discardedQuest)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail("change the progression of $player", e)
            return false
        }
        reload(player)
        return true
    }

    /** Registers the join and quit listener, loads the players already online and starts the flush and ping loops. */
    fun start() {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        Bukkit.getOnlinePlayers().forEach(::startLoading)
        loops += plugin.launch { repeatEvery(FLUSH_INTERVAL) { flushAll() } }
        loops += plugin.launch { repeatEvery(PING_INTERVAL) { if (!Progression.isAvailable) ping() } }
    }

    /** Stops the loops and the running loads, then sends every buffered delta. */
    suspend fun stop() {
        loops.forEach { it.cancel() }
        loads.values.forEach { it.cancel() }
        flushAll()
    }

    /** Starts loading the state of the joining player. */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) {
        startLoading(event.player)
    }

    /** Stops loading the state of the leaving player, drops their cache and sends their buffered deltas. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        val id = event.player.uniqueId
        loads.remove(id)?.cancel()
        players.remove(id)
        plugin.launch {
            flush(id)
            if (Bukkit.getPlayer(id) != null) return@launch
            if (buffers.computeIfPresent(id) { _, buffer -> buffer.takeUnless { it.isEmpty() } } == null) {
                locks.remove(id)
            }
        }
    }

    /** Launches the load of [player]: retries until it succeeds, then loads once more after the join delay. */
    private fun startLoading(player: Player) {
        val id = player.uniqueId
        val joinedAt = TimeSource.Monotonic.markNow()
        lateinit var job: Job
        job = plugin.launch {
            try {
                val loaded = loadWithBackoff(id) ?: return@launch
                Bukkit.getPlayer(id)?.let { onLoaded(it, loaded) }
                val wait = RELOAD_AFTER_JOIN - joinedAt.elapsedNow()
                if (wait.isPositive()) delay(wait)
                reload(id)
            } finally {
                loads.remove(id, job)
            }
        }
        loads.put(id, job)?.cancel()
    }

    /**
     * Loads the state of [player] until it succeeds or the player leaves, waiting longer after each failure.
     *
     * @return the cached progression, or `null` if the player left first
     */
    private suspend fun loadWithBackoff(player: UUID): PlayerProgression? {
        var wait = FIRST_RETRY
        while (Bukkit.getPlayer(player) != null) {
            try {
                val progression = lockOf(player).withLock {
                    PlayerProgression(bridge.loadState(player), clock).also {
                        it.replace(it.state, bufferOf(player).pending())
                    }
                }
                Progression.markAvailable()
                if (Bukkit.getPlayer(player) == null) return null
                players[player] = progression
                return progression
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("load the progression of $player", e)
            }
            delay(wait)
            wait = (wait * 2).coerceAtMost(MAX_RETRY)
        }
        return null
    }

    /** Loads the state of [player] again and replaces the cache unless a local change happened meanwhile. */
    private suspend fun reload(player: UUID) {
        repeat(RELOAD_ATTEMPTS) {
            val cached = players[player] ?: return
            val revision = cached.revision
            try {
                val replaced = lockOf(player).withLock {
                    cached.replace(bridge.loadState(player), bufferOf(player).pending(), revision)
                }
                Progression.markAvailable()
                if (replaced) return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("reload the progression of $player", e)
                return
            }
        }
    }

    /** Sends the buffered deltas of every player that has some. */
    private suspend fun flushAll() {
        buffers.keys.toList().forEach { flush(it) }
    }

    /** Sends the buffered deltas of [player]; on failure they are queued again. */
    private suspend fun flush(player: UUID) {
        val buffer = buffers[player] ?: return
        lockOf(player).withLock {
            val deltas = buffer.drain()
            if (deltas.isEmpty()) return
            try {
                bridge.applyCounterDeltas(player, deltas)
                Progression.markAvailable()
            } catch (e: CancellationException) {
                buffer.requeue(deltas)
                throw e
            } catch (e: Exception) {
                buffer.requeue(deltas)
                fail("send ${deltas.size} counter deltas of $player", e)
            }
        }
    }

    /** Calls the progression service with a read that changes nothing and marks it available if it answers. */
    private suspend fun ping() {
        try {
            bridge.lookupTool(PING_TOOL)
            Progression.markAvailable()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            plugin.logger.fine("The progression service is still unreachable: ${e.message}")
        }
    }

    /** Marks the progression service unavailable and logs the failed call once per outage. */
    private fun fail(action: String, error: Exception) {
        Progression.reportFailure(plugin.logger, action, error)
    }

    /** Returns the delta buffer of [player], creating it if needed. */
    private fun bufferOf(player: UUID) = buffers.computeIfAbsent(player) { CounterBuffer() }

    /** Returns the flush and reload lock of [player], creating it if needed. */
    private fun lockOf(player: UUID) = locks.computeIfAbsent(player) { Mutex() }

    /** Runs [action] every [interval] until the coroutine is cancelled; failures of one run are logged. */
    private suspend fun repeatEvery(interval: Duration, action: suspend () -> Unit) {
        while (true) {
            delay(interval)
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "A progression background task failed", e)
            }
        }
    }

    /** The intervals of the store. */
    companion object {
        /** The delay after a join before the state is loaded once more. */
        val RELOAD_AFTER_JOIN: Duration = 3.seconds

        /** The interval in which buffered counter deltas are sent. */
        val FLUSH_INTERVAL: Duration = 5.seconds

        /** The interval in which an unreachable progression service is pinged. */
        val PING_INTERVAL: Duration = 10.seconds

        /** The delay before the first retry of a failed load. */
        val FIRST_RETRY: Duration = 1.seconds

        /** The longest delay between two retries of a failed load. */
        val MAX_RETRY: Duration = 30.seconds

        /** How often a reload is tried when local changes keep interrupting it. */
        const val RELOAD_ATTEMPTS = 3

        /** The tool id the ping looks up; no tool has it. */
        private val PING_TOOL = UUID(0L, 0L)
    }
}
