package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.ProgressionView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestStateView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.RequirementContext
import java.time.Instant
import java.util.*

/** Decides which quests an NPC offers to a player and which of the player's active quests continue at it. */
object QuestAvailability {
    /**
     * Lists the quests [npc] offers to [player], in pack order.
     *
     * A quest is offered when its requirement holds and the player has no state of it, or only an abandoned one.
     * A recurring quest is offered only when it is among the player's draws of the current period, and a state of an
     * earlier period counts as no state.
     *
     * @param npc the giver
     * @param view the player's progression
     * @param ctx the player, for the requirements
     * @param player the player's UUID, which the recurring draw depends on
     * @param now the current time
     * @param registry the installed definitions
     */
    fun offeredBy(
        npc: NpcDefinition,
        view: ProgressionView,
        ctx: RequirementContext,
        player: UUID,
        now: Instant,
        registry: ProgressionRegistry,
    ): List<QuestDefinition> =
        registry.questsGivenBy(npc).filter { isOffered(it, view, ctx, player, now, registry) }

    /**
     * Checks whether [quest] is offered to [player] by its giver, by the rules of [offeredBy].
     *
     * @param quest the quest
     * @param view the player's progression
     * @param ctx the player, for the requirement
     * @param player the player's UUID, which the recurring draw depends on
     * @param now the current time
     * @param registry the installed definitions
     */
    fun isOffered(
        quest: QuestDefinition,
        view: ProgressionView,
        ctx: RequirementContext,
        player: UUID,
        now: Instant,
        registry: ProgressionRegistry,
    ): Boolean {
        val state = view.questState(quest.id)
        if (quest.type.recurring) {
            val period = QuestProgressView.periodOf(quest, now)
            if (!isFree(state, period)) return false
            val drawn = RecurringDraw.draw(
                player, quest.type, period, registry.pool(quest.type), registry.pools.countFor(quest.type)
            )
            if (drawn.none { it.id == quest.id }) return false
        } else if (!isFree(state, null)) {
            return false
        }
        return quest.requirement.isMet(ctx)
    }

    /**
     * Lists the active quests whose current step is turned in at [npc], in pack order. Recurring quests of an earlier
     * period are not active.
     *
     * @param npc the NPC
     * @param view the player's progression
     * @param now the current time
     * @param registry the installed definitions
     */
    fun activeAt(
        npc: NpcDefinition,
        view: ProgressionView,
        now: Instant,
        registry: ProgressionRegistry,
    ): List<QuestDefinition> = registry.allQuests.filter { quest ->
        val state = view.questState(quest.id) ?: return@filter false
        if (state.status != QuestStatus.ACTIVE) return@filter false
        if (quest.type.recurring && state.period != QuestProgressView.periodOf(quest, now)) return@filter false
        quest.steps.getOrNull(state.step)?.npc?.id == npc.id
    }

    /**
     * Returns whether [state] leaves the quest open for accepting: there is none, it is abandoned, or it belongs to
     * another period than [period].
     *
     * @param period the current period for recurring quests, `null` for quests that are done once
     */
    private fun isFree(state: QuestStateView?, period: Long?): Boolean =
        state == null || state.status == QuestStatus.ABANDONED || (period != null && state.period != period)
}
