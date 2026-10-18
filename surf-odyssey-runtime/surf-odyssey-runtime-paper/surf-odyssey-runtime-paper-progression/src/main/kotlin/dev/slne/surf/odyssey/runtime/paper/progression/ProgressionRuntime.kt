package dev.slne.surf.odyssey.runtime.paper.progression

import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import dev.slne.surf.odyssey.runtime.paper.progression.npc.ProgressionNpcs
import dev.slne.surf.odyssey.runtime.paper.progression.menu.ProgressionPlaceholders
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationRunner
import dev.slne.surf.odyssey.runtime.paper.progression.state.ProgressionStore
import java.time.Clock

/** Which parts of the progression engine a server runs. */
enum class ProgressionMode {
    /** Everything: progress, quests, stations, traders and the views. */
    FULL,

    /** Only showing and managing progression, without making progress. */
    VIEW_ONLY
}

/**
 * The installed progression engine of this server.
 *
 * @property registry every definition of the installed content
 * @property mode which parts of the engine run
 * @property store the cached progression of the players
 * @property operations runs item operations and delivery claims
 * @property clock the clock every time of the engine comes from
 */
class ProgressionRuntime(
    val registry: ProgressionRegistry,
    val mode: ProgressionMode,
    val store: ProgressionStore,
    val operations: OperationRunner,
    val clock: Clock,
) {
    /**
     * Removes the spawned NPCs and a standalone placeholder expansion, stops the background work of the engine and
     * sends every buffered counter delta.
     */
    suspend fun shutdown() {
        ProgressionNpcs.despawnAll()
        ProgressionPlaceholders.unregisterStandalone()
        store.stop()
    }
}
