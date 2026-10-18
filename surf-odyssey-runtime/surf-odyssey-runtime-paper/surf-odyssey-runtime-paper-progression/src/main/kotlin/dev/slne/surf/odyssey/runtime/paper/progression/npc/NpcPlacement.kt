package dev.slne.surf.odyssey.runtime.paper.progression.npc

import dev.slne.surf.odyssey.runtime.paper.progression.SpawnPoint
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition

/**
 * An NPC placed in the main world of a server.
 *
 * @property npc the NPC
 * @property position where the NPC stands and which way it looks
 */
data class NpcPlacement(val npc: NpcDefinition, val position: SpawnPoint)
