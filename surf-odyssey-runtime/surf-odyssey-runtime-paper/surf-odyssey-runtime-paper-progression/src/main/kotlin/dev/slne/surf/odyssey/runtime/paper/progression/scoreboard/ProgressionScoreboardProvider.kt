package dev.slne.surf.odyssey.runtime.paper.progression.scoreboard

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.odyssey.runtime.paper.common.scoreboard.ScoreboardProvider
import dev.slne.surf.odyssey.runtime.paper.common.scoreboard.ScoreboardQuest
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestProgress
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestProgressView
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import org.bukkit.entity.Player

/** Builds the quest block of the scoreboard from a quest definition and its progress. */
object ScoreboardQuestText {
    /** Replaces every line break with a space. */
    private val ONE_LINE: TextReplacementConfig = TextReplacementConfig.builder()
        .match("\\R")
        .replacement(" ")
        .build()

    /**
     * Returns the quest name and the current objective of [quest] with one count per objective of the current step.
     *
     * @return the block, or `null` if [quest] or [progress] is `null` or the current step does not exist
     */
    fun of(quest: QuestDefinition?, progress: QuestProgress?): ScoreboardQuest? {
        if (quest == null || progress == null) return null
        val step = quest.steps.getOrNull(progress.step) ?: return null
        val objective = buildText {
            append(step.description)
            progress.counts.forEachIndexed { index, count ->
                spacer(" · ")
                variableValue("$count/${step.objectives[index].amount}")
            }
        }.replaceText(ONE_LINE)
        return ScoreboardQuest(quest.title.replaceText(ONE_LINE), objective)
    }
}

/** Supplies the active title and the tracked quest of a player from the progression engine. */
object ProgressionScoreboardProvider : ScoreboardProvider {
    /**
     * Returns the display of the title [player] shows, or `null` if there is none, the state is not loaded or the
     * progression service is unreachable.
     */
    override fun titleOf(player: Player): Component? {
        val runtime = Progression.runtimeOrNull ?: return null
        if (!runtime.store.isAvailableFor(player.uniqueId)) return null
        val titleId = runtime.store[player.uniqueId]?.state?.activeTitle ?: return null
        return runtime.registry.title(titleId)?.display
    }

    /**
     * Returns the quest [player] tracks, or `null` if there is none, the state is not loaded or the progression
     * service is unreachable.
     */
    override fun questOf(player: Player): ScoreboardQuest? {
        val runtime = Progression.runtimeOrNull ?: return null
        if (!runtime.store.isAvailableFor(player.uniqueId)) return null
        val state = runtime.store[player.uniqueId]?.state ?: return null
        val quest = state.trackedQuest?.let(runtime.registry::quest) ?: return null
        val stored = state.quests.find { it.questId == quest.id }
        val period = QuestProgressView.periodOf(quest, runtime.clock.instant())
        return ScoreboardQuestText.of(quest, QuestProgressView.compute(quest, stored, state.counters, period))
    }
}
