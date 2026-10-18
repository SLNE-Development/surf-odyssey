package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition

/** One answer of the greeting node that opens a conversation with an NPC. */
sealed interface GreetingAnswer {
    /**
     * Shows the offer node of a quest the NPC offers.
     *
     * @property quest the offered quest
     */
    data class Offer(val quest: QuestDefinition) : GreetingAnswer

    /**
     * Shows the entry node of the current step of an active quest that continues at the NPC.
     *
     * @property quest the active quest
     */
    data class Continue(val quest: QuestDefinition) : GreetingAnswer

    /** Opens the trade menu of the NPC. */
    data object Trade : GreetingAnswer

    /** Opens the station of the NPC. */
    data object Craft : GreetingAnswer

    /** Ends the conversation. */
    data object Leave : GreetingAnswer
}

/** Lists the answers of the greeting node. */
object GreetingNode {
    /**
     * Returns the answers of the greeting of [npc]: the offered quests, then the active quests continuing at the NPC,
     * then trading if the NPC has offers, crafting if it has a station, and leaving last.
     *
     * @param npc the NPC greeting the player
     * @param offered the quests the NPC offers to the player, in display order
     * @param active the player's active quests whose current step continues at the NPC, in display order
     */
    fun answers(npc: NpcDefinition, offered: List<QuestDefinition>, active: List<QuestDefinition>): List<GreetingAnswer> =
        buildList {
            offered.forEach { add(GreetingAnswer.Offer(it)) }
            active.forEach { add(GreetingAnswer.Continue(it)) }
            if (npc.trades.isNotEmpty()) add(GreetingAnswer.Trade)
            if (npc.station != null) add(GreetingAnswer.Craft)
            add(GreetingAnswer.Leave)
        }
}
