package dev.slne.surf.odyssey.runtime.paper.progression.tool

import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessorySlot
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
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
