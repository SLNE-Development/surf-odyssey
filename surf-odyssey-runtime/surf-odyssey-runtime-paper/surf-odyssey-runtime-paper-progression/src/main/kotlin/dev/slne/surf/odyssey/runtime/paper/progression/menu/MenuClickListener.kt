@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.menu

import dev.slne.surf.odyssey.runtime.paper.progression.delivery.openPickupBox
import io.papermc.paper.connection.PlayerGameConnection
import io.papermc.paper.event.player.PlayerCustomClickEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Answers the custom clicks of the [OdysseyMenuDialog] by opening the matching dialog or view on the player's
 * thread.
 *
 * @param plugin the plugin whose scheduler opens the dialogs and views
 */
class MenuClickListener(private val plugin: Plugin) : Listener {
    /** Opens what the clicked button of the Odyssey menu asks for; other custom clicks are ignored. */
    @EventHandler
    fun onCustomClick(event: PlayerCustomClickEvent) {
        val player = (event.commonConnection as? PlayerGameConnection)?.player ?: return
        val open: (Player) -> Unit = when (event.identifier) {
            OdysseyMenuDialog.OPEN_PICKUP -> ::openPickupBox
            else -> return
        }
        player.scheduler.run(plugin, { if (player.isOnline) open(player) }, null)
    }
}
