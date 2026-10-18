package dev.slne.surf.odyssey.runtime.paper.progression.definition.quest

/**
 * Kind of a quest.
 *
 * @property abandonable whether a player may abandon the quest
 * @property recurring whether the quest is drawn from a pool each period instead of being done once
 */
enum class QuestType(val abandonable: Boolean, val recurring: Boolean) {
    /** Main story quest; done once and cannot be abandoned. */
    MAIN(false, false),

    /** Optional quest; done once and can be abandoned. */
    SIDE(true, false),

    /** Drawn from the daily pool each day. */
    DAILY(true, true),

    /** Drawn from the weekly pool each week. */
    WEEKLY(true, true),

    /** Drawn from the monthly pool each month. */
    MONTHLY(true, true),
}
