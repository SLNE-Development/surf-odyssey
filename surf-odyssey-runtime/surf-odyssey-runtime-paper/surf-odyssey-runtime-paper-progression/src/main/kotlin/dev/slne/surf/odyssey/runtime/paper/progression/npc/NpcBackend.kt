package dev.slne.surf.odyssey.runtime.paper.progression.npc

import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import org.bukkit.Location
import org.bukkit.entity.Player

/** An NPC spawned by an [NpcBackend]. */
interface SpawnedNpc {
    /** The definition the NPC was spawned from. */
    val definition: NpcDefinition

    /** Where the NPC stands. */
    val location: Location
}

/**
 * Spawns NPCs and controls which players see them. A spawned NPC is seen by no player until [setVisible] shows it.
 */
interface NpcBackend {
    /**
     * Spawns an NPC that is not persisted and is seen by nobody yet.
     *
     * @param definition the NPC
     * @param location where the NPC stands and which way it looks
     * @return the spawned NPC
     */
    fun spawn(definition: NpcDefinition, location: Location): SpawnedNpc

    /**
     * Shows [npc] to [player] or hides it. Does nothing if the NPC is already in that state for the player.
     *
     * @param npc an NPC spawned by this backend
     * @param player the viewer
     * @param visible whether the player sees the NPC
     */
    fun setVisible(npc: SpawnedNpc, player: Player, visible: Boolean)

    /** Removes every NPC this backend spawned. */
    fun despawnAll()
}
