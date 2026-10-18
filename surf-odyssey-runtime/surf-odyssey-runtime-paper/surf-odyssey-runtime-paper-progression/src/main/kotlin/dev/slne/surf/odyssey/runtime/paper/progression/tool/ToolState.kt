package dev.slne.surf.odyssey.runtime.paper.progression.tool

import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessoryDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessorySlot
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationOutcome
import java.util.*

/**
 * The state a tool item carries.
 *
 * @property toolId the ID of the tool definition
 * @property uuid the unique ID of this item
 * @property usage the total usage of the item
 * @property chronicle the history of the item
 * @property accessories the ID of the attached accessory per slot ID
 */
data class ToolState(
    val toolId: String,
    val uuid: UUID,
    val usage: Long,
    val chronicle: Chronicle,
    val accessories: Map<String, String>,
)

/** Answers which accessory slots of a tool are open. */
object ToolSlots {
    /**
     * Returns the slots of [tool] that are open at [level], ordered by the level that opens them.
     *
     * @param tool the tool definition
     * @param level the current level of the tool
     */
    fun unlocked(tool: ToolDefinition, level: Int): List<AccessorySlot> =
        tool.slots.entries.filter { it.key <= level }.sortedBy { it.key }.map { it.value }
}

/** What happens to a detached accessory after the operation that gives it to the player. */
enum class RemovalResult {
    /** The operation was stored; the accessory is in the inventory or the pickup box. */
    DELIVERED,

    /** The operation was definitely not stored; the accessory goes back into its slot. */
    REATTACH,

    /** The service could not be reached, so the operation may still be stored; the slot stays empty. */
    PENDING,
}

/** Attaches accessories to and detaches them from the slots of a tool state. */
object AccessoryRules {
    /**
     * Returns [state] with [accessory] attached to the slot [slotId], or `null` if the slot is not open at the level
     * of the tool, the accessory does not fit the slot or the slot is filled.
     *
     * @param tool the tool definition
     * @param state the state of the tool item
     * @param slotId the ID of the slot
     * @param accessory the accessory to attach
     */
    fun attach(tool: ToolDefinition, state: ToolState, slotId: String, accessory: AccessoryDefinition): ToolState? {
        val level = ToolProgress.levelFor(tool, state.usage)
        if (ToolSlots.unlocked(tool, level).none { it.id == slotId }) return null
        if (accessory.slot.id != slotId || slotId in state.accessories) return null
        return state.copy(accessories = state.accessories + (slotId to accessory.id))
    }

    /**
     * Returns [state] with the slot [slotId] emptied together with the ID of the accessory that was attached there,
     * or `null` if the slot is empty.
     *
     * @param state the state of the tool item
     * @param slotId the ID of the slot
     */
    fun detach(state: ToolState, slotId: String): Pair<ToolState, String>? {
        val accessoryId = state.accessories[slotId] ?: return null
        return state.copy(accessories = state.accessories - slotId) to accessoryId
    }

    /**
     * Returns what happens to a detached accessory after the operation that gives it to the player ended with
     * [outcome]: [RemovalResult.DELIVERED] on success, [RemovalResult.REATTACH] on busy, missing items or rejection,
     * and [RemovalResult.PENDING] when the service could not be reached.
     *
     * @param outcome the outcome of the operation
     */
    fun afterRemoval(outcome: OperationOutcome): RemovalResult = when (outcome) {
        OperationOutcome.Success -> RemovalResult.DELIVERED
        OperationOutcome.Busy, OperationOutcome.MissingItems, is OperationOutcome.Rejected -> RemovalResult.REATTACH
        OperationOutcome.Unavailable -> RemovalResult.PENDING
    }
}
