package dev.slne.surf.odyssey.runtime.paper.hub.common.progression

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.ForestRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.HarvestRegion
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.MineRegion
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import dev.slne.surf.odyssey.runtime.paper.progression.quest.ProgressEvent
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestEngine
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ChronicleEvent
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ToolUsage
import org.bukkit.entity.Player

/** Turns successful actions in hub regions into quest progress and tool usage. */
object RegionProgress {
    /** The fish weight from which on a catch is worth a single use. */
    private const val COMMON_WEIGHT = 30

    /** How much lower a weight must be for each further use a catch is worth. */
    private const val WEIGHT_STEP = 10

    /**
     * Returns the tool type that harvesting in [region] levels: pickaxes in mines, axes in forests.
     *
     * @return the tool type, or `null` for other harvest regions
     */
    fun harvestTool(region: HarvestRegion): ToolType? = when (region) {
        is MineRegion -> ToolType.PICKAXE
        is ForestRegion -> ToolType.AXE
        else -> null
    }

    /**
     * Returns the rod usage a catch of a fish with [weight] is worth: 1 for weights of 30 and more, and one more for
     * each 10 the weight lies below 30.
     *
     * @param weight the relative weight of the caught fish
     */
    fun catchUsage(weight: Int): Int = maxOf(1, (COMMON_WEIGHT - weight) / WEIGHT_STEP + 1)

    /**
     * Reports to the quest engine that [player] harvested [event] and adds one use to the tool of [region] the player
     * holds. Must run on the thread of [player].
     *
     * @param player the harvesting player
     * @param region the region the block belongs to
     * @param event what was harvested
     */
    fun harvested(player: Player, region: HarvestRegion, event: ProgressEvent.Harvested) {
        QuestEngine.onProgress(player, event)
        harvestTool(region)?.let { ToolUsage.record(player, it, 1, ChronicleEvent.BlockMined) }
    }

    /**
     * Reports to the quest engine that [player] caught [event] and adds the usage of a fish with [weight] to the rod
     * the player holds. Must run on the thread of [player].
     *
     * @param player the fishing player
     * @param event what was caught
     * @param weight the relative weight of the caught fish
     * @param fishName the name of the catch kept in the rod's chronicle
     */
    fun fished(player: Player, event: ProgressEvent.Fished, weight: Int, fishName: String) {
        QuestEngine.onProgress(player, event)
        ToolUsage.record(player, ToolType.ROD, catchUsage(weight), ChronicleEvent.Catch(fishName))
    }

    /**
     * Reports to the quest engine that [player] killed [event] and, if [weapon] is set, adds one use to the weapon of
     * that type the player holds. Must run on the thread of [player].
     *
     * @param player the killer
     * @param event what was killed
     * @param weapon the tool type the kill levels, or `null` if it levels none
     */
    fun killed(player: Player, event: ProgressEvent.Killed, weapon: ToolType?) {
        QuestEngine.onProgress(player, event)
        weapon?.let { ToolUsage.record(player, it, 1, ChronicleEvent.MobKilled) }
    }
}
