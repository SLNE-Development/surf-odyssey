package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import java.util.UUID
import kotlin.random.Random

/** Draws the recurring quests a player gets for a period. */
object RecurringDraw {
    /**
     * Draws [count] distinct quests from [pool] for [player] in [period].
     *
     * The result depends only on the player, the quest type, the period and the set of pool IDs, never on the order of
     * [pool] or on the moment of the call. If the pool holds fewer than [count] quests, all of them are returned.
     *
     * @param player the player the draw belongs to
     * @param type the recurring quest type
     * @param period the period index as returned by [Periods.index]
     * @param pool the candidate quests
     * @param count how many quests to draw
     * @return the drawn quests
     */
    fun draw(
        player: UUID,
        type: QuestType,
        period: Long,
        pool: List<QuestDefinition>,
        count: Int,
    ): List<QuestDefinition> {
        val seed = player.mostSignificantBits xor player.leastSignificantBits xor (period * 31 + type.ordinal)
        return pool.sortedBy { it.id }.shuffled(Random(seed)).take(count)
    }
}
