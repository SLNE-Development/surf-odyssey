package dev.slne.surf.odyssey.core.common.progression

import dev.slne.surf.api.core.util.requiredService
import java.util.*

/** The progression bridge implementation loaded from the classpath. */
private val bridge = requiredService<ProgressionBridge>()

/** Gives the plugins access to the progression service; every function mirrors [ProgressionService]. */
interface ProgressionBridge {
    /** Returns everything stored about the progression of [player]. */
    suspend fun loadState(player: UUID): ProgressionStateDto

    /** Adds each delta to its counter, creating counters that do not exist yet. */
    suspend fun applyCounterDeltas(player: UUID, deltas: List<CounterDeltaDto>)

    /** Stores all changes and deliveries of [operation] completely or not at all; repeated ids return the first outcome. */
    suspend fun commitOperation(operation: OperationDto): OperationResultDto

    /** Claims an unclaimed delivery and returns its Base64 item to the single winning claim, otherwise `null`. */
    suspend fun claimDelivery(player: UUID, deliveryId: UUID): String?

    /** Abandons an active quest and discards its counters; returns `true` if the quest was active. */
    suspend fun abandonQuest(player: UUID, questId: String): Boolean

    /** Sets the quest [player] tracks, or clears it when [questId] is `null`. */
    suspend fun setTrackedQuest(player: UUID, questId: String?)

    /** Sets or clears the title [player] shows; returns `false` if the title is not unlocked. */
    suspend fun setActiveTitle(player: UUID, titleId: String?): Boolean

    /** Registers a newly created tool item; registering a known tool changes nothing. */
    suspend fun registerTool(tool: UUID, toolId: String, owner: UUID)

    /** Records that [holder] holds [tool] on [server] and checks the recent sightings for a duplicated item. */
    suspend fun reportToolSighting(tool: UUID, holder: UUID, server: String): ToolSightingResult

    /** Returns the registration of [tool], or `null` if it is not registered. */
    suspend fun lookupTool(tool: UUID): ToolRecordDto?

    /** Deletes the state and the counters of a quest, as if [player] had never accepted it. */
    suspend fun resetQuest(player: UUID, questId: String)

    /** Locks a recipe for [player] again. */
    suspend fun revokeRecipe(player: UUID, recipeId: String)

    /** Takes a title away from [player], clearing it when it is the active title. */
    suspend fun revokeTitle(player: UUID, titleId: String)

    /** Delegates every call to the loaded bridge implementation. */
    companion object : ProgressionBridge by bridge {
        /** The loaded bridge implementation. */
        val INSTANCE get() = bridge
    }
}
