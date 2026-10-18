package dev.slne.surf.odyssey.runtime.paper.progression.state

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.ProgressionView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.RequirementContext
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import dev.slne.surf.odyssey.runtime.paper.progression.operation.PlayerEscrowInventory
import org.bukkit.entity.Player

/** Looks up the level of the progression tool a player holds. */
fun interface ToolLevelLookup {
    /**
     * Returns the level of the progression tool of [type] that [player] holds in the main hand.
     *
     * @param player the player
     * @param type the tool type
     * @return the level, or `null` if the player holds no tool of this type
     */
    fun levelInHand(player: Player, type: ToolType): Int?
}

/**
 * The [RequirementContext] of an online player. Must be used from the player's thread.
 *
 * Items are counted in the storage slots of the inventory, tool levels come from [toolLevels].
 *
 * @param player the player
 * @property progression the player's cached progression
 */
class PlayerRequirementContext(
    private val player: Player,
    override val progression: ProgressionView,
) : RequirementContext {
    /** Counts the matching items in the storage slots of the player's inventory. */
    override fun countItems(ref: ItemRef): Int = PlayerEscrowInventory(player).count(ref)

    /** Returns the level [toolLevels] reports for the tool in the player's main hand. */
    override fun toolLevelInHand(type: ToolType): Int? = toolLevels.levelInHand(player, type)

    /** Holds the tool level source shared by all contexts. */
    companion object {
        /** Reports tool levels; until tools are installed it reports that no tool is held. */
        @Volatile
        var toolLevels: ToolLevelLookup = ToolLevelLookup { _, _ -> null }
    }
}
