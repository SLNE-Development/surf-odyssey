package dev.slne.surf.odyssey.core.common.progression

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.rabbitmq.api.rpc.RpcService

/** Stores the quests, recipes, titles, deliveries, station jobs and tools of all players. */
@RpcService
interface ProgressionService {
    /** Returns everything stored about the progression of [player]. */
    suspend fun loadState(player: SerializableUUID): ProgressionStateDto

    /** Adds each delta to its counter, creating counters that do not exist yet. */
    suspend fun applyCounterDeltas(player: SerializableUUID, deltas: List<CounterDeltaDto>)

    /**
     * Stores all changes and deliveries of [operation] in one transaction, or nothing when one change cannot be
     * applied. Committing an operation id a second time stores nothing and returns the first outcome again.
     */
    suspend fun commitOperation(operation: OperationDto): OperationResultDto

    /**
     * Claims an unclaimed delivery of [player]. Only one of several concurrent claims wins.
     *
     * @return the Base64 item of the delivery for the winning claim, or `null` when the delivery is unknown, belongs
     *         to another player or was already claimed
     */
    suspend fun claimDelivery(player: SerializableUUID, deliveryId: SerializableUUID): String?

    /**
     * Abandons an active quest and discards its counters.
     *
     * @return `true` if the quest was active and is now abandoned
     */
    suspend fun abandonQuest(player: SerializableUUID, questId: String): Boolean

    /** Sets the quest [player] tracks, or clears it when [questId] is `null`. */
    suspend fun setTrackedQuest(player: SerializableUUID, questId: String?)

    /**
     * Sets the title [player] shows, or clears it when [titleId] is `null`.
     *
     * @return `false` if [titleId] is a title the player has not unlocked
     */
    suspend fun setActiveTitle(player: SerializableUUID, titleId: String?): Boolean

    /** Registers a newly created tool item; registering a known tool changes nothing. */
    suspend fun registerTool(tool: SerializableUUID, toolId: String, owner: SerializableUUID)

    /** Records that [holder] holds [tool] on [server] and checks the recent sightings for a duplicated item. */
    suspend fun reportToolSighting(tool: SerializableUUID, holder: SerializableUUID, server: String): ToolSightingResult

    /** Returns the registration of [tool], or `null` if it is not registered. */
    suspend fun lookupTool(tool: SerializableUUID): ToolRecordDto?

    /** Deletes the state and the counters of a quest, as if [player] had never accepted it. */
    suspend fun resetQuest(player: SerializableUUID, questId: String)

    /** Locks a recipe for [player] again. */
    suspend fun revokeRecipe(player: SerializableUUID, recipeId: String)

    /** Takes a title away from [player], clearing it when it is the active title. */
    suspend fun revokeTitle(player: SerializableUUID, titleId: String)
}
