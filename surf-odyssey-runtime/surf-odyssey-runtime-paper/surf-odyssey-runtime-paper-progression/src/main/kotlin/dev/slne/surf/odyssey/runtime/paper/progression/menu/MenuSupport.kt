@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.menu

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.dialog.noticeDialog
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import org.bukkit.entity.Player

/**
 * Runs [action] in a coroutine on the thread of [player], for menu buttons that call the progression service.
 *
 * @param player the player who clicked
 * @param action the work to do; it may suspend
 */
internal fun launchMenuAction(player: Player, action: suspend () -> Unit) {
    val plugin = ProgressionModule.plugin
    plugin.launch(plugin.entityDispatcher(player)) {
        if (player.isOnline) action()
    }
}

/**
 * Shows [player] a dialog titled [title] that only says that the feature is not available right now.
 *
 * @param player the player
 * @param title the title of the dialog that could not be shown
 */
internal fun showUnavailable(player: Player, title: String) {
    player.showDialog(noticeDialog(buildText { primary(title) }, buildText { error("Das ist gerade nicht verfügbar.") }))
}
