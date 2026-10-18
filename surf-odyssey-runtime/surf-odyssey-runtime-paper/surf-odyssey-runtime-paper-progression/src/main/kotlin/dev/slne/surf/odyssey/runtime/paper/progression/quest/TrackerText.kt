package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig

/** Builds the one-line text that shows the tracked quest. */
object TrackerText {
    /** Replaces every line break with a space. */
    private val ONE_LINE: TextReplacementConfig = TextReplacementConfig.builder()
        .match("\\R")
        .replacement(" ")
        .build()

    /**
     * Renders the tracked quest as `title · step description · count/amount · ...`, with one count per objective of
     * the current step, on a single line.
     *
     * @param quest the tracked quest, or `null` if none is tracked
     * @param progress the progress of [quest], or `null` if it has none
     * @return the text, or an empty component if [quest] or [progress] is `null`
     */
    fun render(quest: QuestDefinition?, progress: QuestProgress?): Component {
        if (quest == null || progress == null) return Component.empty()
        val step = quest.steps.getOrNull(progress.step) ?: return Component.empty()
        return buildText {
            append(quest.title)
            spacer(" · ")
            append(step.description)
            progress.counts.forEachIndexed { index, count ->
                spacer(" · ")
                variableValue("$count/${step.objectives[index].amount}")
            }
        }.replaceText(ONE_LINE)
    }
}
