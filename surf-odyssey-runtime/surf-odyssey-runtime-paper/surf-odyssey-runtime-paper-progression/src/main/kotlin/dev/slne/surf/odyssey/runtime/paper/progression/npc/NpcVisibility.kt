package dev.slne.surf.odyssey.runtime.paper.progression.npc

import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.RequirementContext
import dev.slne.surf.odyssey.runtime.paper.progression.state.PlayerRequirementContext
import dev.slne.surf.odyssey.runtime.paper.progression.state.ProgressionStore
import org.bukkit.entity.Player

/**
 * Decides which spawned NPCs each player sees and applies it through the backend.
 *
 * An NPC without a visibility requirement is seen by everyone. An NPC with one is seen only by players whose
 * progression is loaded and meets it.
 *
 * @param backend the backend that spawned [npcs]
 * @param npcs the spawned NPCs
 * @param store the cached progression of the players
 */
class NpcVisibility(
    private val backend: NpcBackend,
    private val npcs: List<SpawnedNpc>,
    private val store: ProgressionStore,
) {
    /**
     * Shows [player] the NPCs they may see and hides the others. Must run on the player's thread, because the
     * requirements may look at the inventory.
     *
     * @param player the viewer
     */
    fun refresh(player: Player) {
        val context = store[player.uniqueId]?.let { PlayerRequirementContext(player, it) }
        val decided = mutableMapOf<String, Boolean>()
        for (npc in npcs) {
            val visible = decided.getOrPut(npc.definition.id) { isVisible(npc.definition, context) }
            backend.setVisible(npc, player, visible)
        }
    }

    /**
     * Hides every NPC from [player].
     *
     * @param player the viewer
     */
    fun hideAll(player: Player) {
        npcs.forEach { backend.setVisible(it, player, false) }
    }

    /** Holds the visibility rule. */
    companion object {
        /**
         * Returns whether a player sees [npc].
         *
         * @param npc the NPC
         * @param context the player's requirement context, or `null` while their progression is not loaded
         * @return `true` if the NPC has no visibility requirement, or the context exists and meets it
         */
        fun isVisible(npc: NpcDefinition, context: RequirementContext?): Boolean {
            val requirement = npc.visibleWhen ?: return true
            return context != null && requirement.isMet(context)
        }
    }
}
