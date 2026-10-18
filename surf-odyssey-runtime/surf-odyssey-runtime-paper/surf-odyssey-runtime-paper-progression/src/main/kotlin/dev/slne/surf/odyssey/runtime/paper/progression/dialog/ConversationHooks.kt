package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import org.bukkit.entity.Player

/**
 * What the "Handeln" and "Herstellen" answers of a conversation open. Both are called on the player's thread after
 * the conversation dialog was closed; until a feature sets them they tell the player that it is not available.
 */
object ConversationHooks {
    /** Opens the trade menu of the NPC for the player. */
    @Volatile
    var openTrade: (Player, NpcDefinition) -> Unit = { player, _ -> ProgressionMessages.unavailable(player) }

    /** Opens the station of the NPC for the player. */
    @Volatile
    var openStation: (Player, NpcDefinition) -> Unit = { player, _ -> ProgressionMessages.unavailable(player) }
}
