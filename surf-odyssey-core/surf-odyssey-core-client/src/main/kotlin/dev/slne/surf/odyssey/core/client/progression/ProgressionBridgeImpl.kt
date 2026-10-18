package dev.slne.surf.odyssey.core.client.progression

import com.google.auto.service.AutoService
import dev.slne.surf.odyssey.core.client.OdysseyClientInstance
import dev.slne.surf.odyssey.core.common.progression.CounterDeltaDto
import dev.slne.surf.odyssey.core.common.progression.OperationDto
import dev.slne.surf.odyssey.core.common.progression.OperationResultDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionBridge
import dev.slne.surf.odyssey.core.common.progression.ProgressionService
import dev.slne.surf.odyssey.core.common.progression.ProgressionStateDto
import dev.slne.surf.odyssey.core.common.progression.ToolRecordDto
import dev.slne.surf.odyssey.core.common.progression.ToolSightingResult
import java.util.*

/** The RPC proxy that forwards progression calls to the microservice. */
private val proxy by lazy {
    OdysseyClientInstance.INSTANCE.rabbitApi.createRpcService<ProgressionService>()
}

/** Implements [ProgressionBridge] by forwarding every call to the progression microservice over RabbitMQ. */
@AutoService(ProgressionBridge::class)
class ProgressionBridgeImpl : ProgressionBridge {
    /** Loads the state of [player] from the microservice. */
    override suspend fun loadState(player: UUID): ProgressionStateDto = proxy.loadState(player)

    /** Sends counter deltas of [player] to the microservice. */
    override suspend fun applyCounterDeltas(player: UUID, deltas: List<CounterDeltaDto>) =
        proxy.applyCounterDeltas(player, deltas)

    /** Commits [operation] at the microservice. */
    override suspend fun commitOperation(operation: OperationDto): OperationResultDto =
        proxy.commitOperation(operation)

    /** Claims a delivery of [player] at the microservice. */
    override suspend fun claimDelivery(player: UUID, deliveryId: UUID): String? =
        proxy.claimDelivery(player, deliveryId)

    /** Abandons a quest of [player] at the microservice. */
    override suspend fun abandonQuest(player: UUID, questId: String): Boolean = proxy.abandonQuest(player, questId)

    /** Sets the tracked quest of [player] at the microservice. */
    override suspend fun setTrackedQuest(player: UUID, questId: String?) = proxy.setTrackedQuest(player, questId)

    /** Sets the active title of [player] at the microservice. */
    override suspend fun setActiveTitle(player: UUID, titleId: String?): Boolean =
        proxy.setActiveTitle(player, titleId)

    /** Registers a tool at the microservice. */
    override suspend fun registerTool(tool: UUID, toolId: String, owner: UUID) =
        proxy.registerTool(tool, toolId, owner)

    /** Reports a tool sighting to the microservice. */
    override suspend fun reportToolSighting(tool: UUID, holder: UUID, server: String): ToolSightingResult =
        proxy.reportToolSighting(tool, holder, server)

    /** Looks up a tool at the microservice. */
    override suspend fun lookupTool(tool: UUID): ToolRecordDto? = proxy.lookupTool(tool)

    /** Resets a quest of [player] at the microservice. */
    override suspend fun resetQuest(player: UUID, questId: String) = proxy.resetQuest(player, questId)

    /** Revokes a recipe of [player] at the microservice. */
    override suspend fun revokeRecipe(player: UUID, recipeId: String) = proxy.revokeRecipe(player, recipeId)

    /** Revokes a title of [player] at the microservice. */
    override suspend fun revokeTitle(player: UUID, titleId: String) = proxy.revokeTitle(player, titleId)
}
