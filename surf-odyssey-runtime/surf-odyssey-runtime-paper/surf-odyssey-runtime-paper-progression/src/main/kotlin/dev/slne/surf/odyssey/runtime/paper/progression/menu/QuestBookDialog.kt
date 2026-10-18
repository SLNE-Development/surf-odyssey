@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.menu

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.builder.actionButton
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestEngine
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import org.bukkit.entity.Player
import java.time.Duration

/**
 * The quest book: the active quests of the player with their current step and counts, the recurring quests of the
 * current periods with their remaining time, and the completed quests. Each active quest can be tracked, and
 * abandonable ones can be abandoned after a confirmation. The dialog is built for each show.
 */
object QuestBookDialog {
    /** The width of the text blocks of the dialog. */
    private const val TEXT_WIDTH = 300

    /** The title of the dialog. */
    private const val TITLE = "Auftragsbuch"

    /**
     * Shows the quest book to [player], or a notice that it is not available while the progression service or the
     * player's state is not.
     */
    fun show(player: Player) {
        val runtime = Progression.runtimeOrNull
        val progression = runtime?.store?.get(player.uniqueId)
        if (runtime == null || progression == null || !runtime.store.isAvailableFor(player.uniqueId)) {
            showUnavailable(player, TITLE)
            return
        }
        player.showDialog(build(QuestBookEntries.build(runtime.registry, progression.state, runtime.clock.instant())))
    }

    /** Builds the quest book dialog showing [book]. */
    private fun build(book: QuestBookSections): Dialog = dialog {
        base {
            title { primary(TITLE) }
            body {
                plainMessage(TEXT_WIDTH) { activeSection(book.active) }
                plainMessage(TEXT_WIDTH) { recurringSection(book.recurring) }
                plainMessage(TEXT_WIDTH) { completedSection(book) }
            }
        }
        val buttons = book.active.flatMap(::buttonsOf)
        type {
            if (buttons.isEmpty()) {
                notice { label { spacer("Schließen") } }
            } else {
                multiAction(buttons) {
                    columns(2)
                    exitAction { label { spacer("Schließen") } }
                }
            }
        }
    }

    /** Builds the "Verfolgen" button of [entry] unless it is tracked, and its "Abbrechen" button if it is abandonable. */
    private fun buttonsOf(entry: ActiveQuestEntry): List<ActionButton> = buildList {
        if (!entry.tracked) {
            add(actionButton {
                label { text("Verfolgen: "); append(entry.quest.title) }
                action { playerCallback { player -> track(player, entry.quest) } }
            })
        }
        if (entry.abandonable) {
            add(actionButton {
                label { error("Abbrechen: "); append(entry.quest.title) }
                action { playerCallback { player -> player.showDialog(confirmAbandon(entry.quest)) } }
            })
        }
    }

    /** Builds the dialog that asks whether [quest] should really be abandoned. */
    private fun confirmAbandon(quest: QuestDefinition): Dialog = dialog {
        base {
            title { error("Quest abbrechen") }
            body {
                plainMessage(TEXT_WIDTH) {
                    text("Möchtest du ")
                    append(quest.title)
                    text(" wirklich abbrechen? ")
                    error("Fortschritt geht verloren.")
                }
            }
        }
        type {
            confirmation {
                yes {
                    label { error("Abbrechen") }
                    action { playerCallback { player -> abandon(player, quest) } }
                }
                no {
                    label { text("Zurück") }
                    action {
                        playerCallback { player ->
                            player.playNoSound()
                            show(player)
                        }
                    }
                }
            }
        }
    }

    /** Tracks [quest] for [player] and shows the quest book again. */
    private fun track(player: Player, quest: QuestDefinition) = launchMenuAction(player) {
        QuestEngine.track(player, quest)
        player.playYesSound()
        show(player)
    }

    /** Abandons [quest] for [player] and shows the quest book again. */
    private fun abandon(player: Player, quest: QuestDefinition) = launchMenuAction(player) {
        if (QuestEngine.abandon(player, quest)) player.playYesSound() else player.playNoSound()
        show(player)
    }

    /** Appends the section of the active quests. */
    private fun SurfComponentBuilder.activeSection(active: List<ActiveQuestEntry>) {
        primary("Aktiv")
        if (active.isEmpty()) {
            appendNewline()
            spacer("Keine aktiven Quests.")
        }
        active.forEach { entry ->
            appendNewline()
            typeLabel(entry.quest.type)
            appendSpace()
            append(entry.quest.title)
            if (entry.tracked) success(" (verfolgt)")
            val step = entry.quest.steps[entry.progress.step]
            appendNewline()
            spacer("  ")
            append(step.description)
            entry.progress.counts.forEachIndexed { index, count ->
                spacer(" · ")
                variableValue("$count/${step.objectives[index].amount}")
            }
        }
    }

    /** Appends the section of the recurring quests. */
    private fun SurfComponentBuilder.recurringSection(recurring: List<RecurringQuestEntry>) {
        primary("Wiederkehrend")
        if (recurring.isEmpty()) {
            appendNewline()
            spacer("Keine wiederkehrenden Quests.")
        }
        recurring.forEach { entry ->
            appendNewline()
            typeLabel(entry.quest.type)
            appendSpace()
            append(entry.quest.title)
            spacer(" · ")
            when (entry.status) {
                RecurringStatus.OPEN -> info("Offen")
                RecurringStatus.ACTIVE -> warning("Aktiv")
                RecurringStatus.COMPLETED -> success("Abgeschlossen")
            }
            spacer(" · noch ")
            variableValue(formatRemaining(entry.remaining))
        }
    }

    /** Appends the section of the completed quests. */
    private fun SurfComponentBuilder.completedSection(book: QuestBookSections) {
        primary("Abgeschlossen")
        spacer(" (")
        variableValue(book.completedCount)
        spacer(")")
        book.recentlyCompleted.forEach { quest ->
            appendNewline()
            typeLabel(quest.type)
            appendSpace()
            append(quest.title)
        }
    }

    /** Appends the name of [type] in the color of its kind. */
    private fun SurfComponentBuilder.typeLabel(type: QuestType) {
        when (type) {
            QuestType.MAIN -> warning("[Hauptquest]")
            QuestType.SIDE -> info("[Nebenquest]")
            QuestType.DAILY -> success("[Täglich]")
            QuestType.WEEKLY -> success("[Wöchentlich]")
            QuestType.MONTHLY -> success("[Monatlich]")
        }
    }

    /** Formats [remaining] as days and hours, or hours and minutes below one day, for example `14 Std. 5 Min.`. */
    private fun formatRemaining(remaining: Duration): String {
        val minutes = remaining.toMinutes().coerceAtLeast(0)
        val days = minutes / (24 * 60)
        val hours = minutes / 60 % 24
        return when {
            days > 0 -> "$days T. $hours Std."
            hours > 0 -> "$hours Std. ${minutes % 60} Min."
            else -> "${minutes % 60} Min."
        }
    }
}
