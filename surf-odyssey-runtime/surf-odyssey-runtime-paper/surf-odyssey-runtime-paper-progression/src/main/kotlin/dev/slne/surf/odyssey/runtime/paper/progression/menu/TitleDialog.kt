@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.menu

import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.builder.actionButton
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.definition.title.TitleDefinition
import io.papermc.paper.dialog.Dialog
import org.bukkit.entity.Player

/**
 * The title selection: every title the player earned as a button, the shown one marked, and a button to show no
 * title. The dialog is built for each show.
 */
object TitleDialog {
    /** The title of the dialog. */
    private const val TITLE = "Titel wählen"

    /**
     * Shows the title selection to [player], or a notice that it is not available while the progression service or
     * the player's state is not.
     */
    fun show(player: Player) {
        val runtime = Progression.runtimeOrNull
        val progression = runtime?.store?.get(player.uniqueId)
        if (runtime == null || progression == null || !runtime.store.isAvailableFor(player.uniqueId)) {
            showUnavailable(player, TITLE)
            return
        }
        val state = progression.state
        val earned = runtime.registry.allTitles.filter { it.id in state.titles }
        player.showDialog(build(earned, state.activeTitle))
    }

    /** Builds the dialog listing [earned] with [active] marked. */
    private fun build(earned: List<TitleDefinition>, active: String?): Dialog = dialog {
        base {
            title { primary(TITLE) }
            body {
                plainMessage {
                    if (earned.isEmpty()) spacer("Du hast noch keine Titel verdient.")
                    else info("Wähle den Titel, der neben deinem Namen steht.")
                }
            }
        }
        val buttons = earned.map { title ->
            actionButton {
                label {
                    append(title.display)
                    if (title.id == active) success(" (aktiv)")
                }
                action { playerCallback { player -> select(player, title) } }
            }
        } + actionButton {
            label {
                text("Keinen Titel")
                if (active == null) success(" (aktiv)")
            }
            action { playerCallback { player -> select(player, null) } }
        }
        type {
            multiAction(buttons) {
                columns(1)
                exitAction { label { spacer("Schließen") } }
            }
        }
    }

    /** Shows [title] for [player], or no title when it is `null`, and tells the player the result. */
    private fun select(player: Player, title: TitleDefinition?) = launchMenuAction(player) {
        val store = Progression.runtime.store
        if (store.setActiveTitle(player.uniqueId, title?.id)) {
            player.playYesSound()
            if (title == null) ProgressionMessages.titleCleared(player) else ProgressionMessages.titleChanged(player, title.display)
        } else {
            player.playNoSound()
            if (Progression.isAvailable) ProgressionMessages.titleNotEarned(player) else ProgressionMessages.unavailable(player)
        }
    }
}
