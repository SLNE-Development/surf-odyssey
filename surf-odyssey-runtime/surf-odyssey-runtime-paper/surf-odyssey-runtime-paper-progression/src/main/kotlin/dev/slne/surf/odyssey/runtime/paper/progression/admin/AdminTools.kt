package dev.slne.surf.odyssey.runtime.paper.progression.admin

import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.operation.ItemProducingReward
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ToolItems
import org.bukkit.inventory.ItemStack
import java.util.*

/** Helpers that give tools at a chosen level and change the level of held tools. */
object AdminTools {
    /**
     * Returns the usage a tool needs to be on [level], with [level] limited to the levels of [tool].
     *
     * @param tool the tool definition
     * @param level the wanted level
     */
    fun usageFor(tool: ToolDefinition, level: Int): Long = tool.curve.usageFor(level.coerceIn(1, tool.maxLevel))

    /**
     * Sets the usage of [item] to the usage [level] needs and renders its bonuses and lore again.
     *
     * @param item the tool item, changed in place
     * @param tool the definition of the tool
     * @param level the wanted level, limited to the levels of [tool]
     * @return `false` if [item] is not a tool item
     */
    fun setLevel(item: ItemStack, tool: ToolDefinition, level: Int): Boolean {
        val state = ToolItems.read(item) ?: return false
        val updated = state.copy(usage = usageFor(tool, level))
        ToolItems.write(item, updated)
        ToolItems.refresh(item, tool, updated)
        return true
    }
}

/**
 * Gives a new item of a tool that already is on a level.
 *
 * @property tool the given tool
 * @property level the level the new item starts on, limited to the levels of [tool]
 */
data class LeveledToolReward(val tool: ToolDefinition, val level: Int) : ItemProducingReward {
    /** Always `admin_tool`. */
    override val typeKey: String = "admin_tool"

    /** Creates the tool item for [owner] and sets it to [level]. */
    override fun produce(owner: UUID): List<ItemStack> {
        val item = ToolItems.create(tool, owner)
        AdminTools.setLevel(item, tool, level)
        return listOf(item)
    }
}
