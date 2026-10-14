package dev.slne.surf.odyssey.runtime.paper.common.menu.home.dialog

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.runtime.paper.common.dialog.DialogSchema
import dev.slne.surf.odyssey.runtime.paper.common.dialog.inputDialog
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.launchHomeAction
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.onOwnThread
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.requirePermission
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeDetailView
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeView
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.common.player.cachedPlayer
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry
import io.papermc.paper.dialog.Dialog
import org.bukkit.entity.Player

object HomeDialogSchema : DialogSchema() {
    val name by string("Name") {
        trim()
        nonEmpty("Der Name darf nicht leer sein.")
        max(16, "Der Name darf maximal 16 Zeichen lang sein.")
    }

    val description by string("Beschreibung") {
        multiline(maxLines = 10, height = 300)
        trim()
        max(500, "Die Beschreibung darf maximal 500 Zeichen lang sein.")
    }
}

/** Creates a home at the player's position, or edits the name and description of an existing one. */
object HomeDialog {
    /** Builds the dialog; a null [home] creates a new home at the player's position when submitted. */
    fun open(home: Home? = null): Dialog = inputDialog(HomeDialogSchema) {
        title { primary(if (home == null) "Home erstellen" else "Home bearbeiten") }

        initial(HomeDialogSchema.name, home?.name)
        initial(HomeDialogSchema.description, home?.description)

        validate { player, values ->
            val name = values[HomeDialogSchema.name]

            if (player.cachedPlayer()?.hasHomeNamed(name, except = home) == true) {
                mapOf(HomeDialogSchema.name to "Du hast bereits ein Home mit diesem Namen.")
            } else {
                emptyMap()
            }
        }

        submit(if (home == null) "Erstellen" else "Speichern") { player, values ->
            val permission = if (home == null) {
                OdysseyBasePermissionRegistry.HOME_CREATE
            } else {
                OdysseyBasePermissionRegistry.HOME_EDIT
            }

            if (!player.requirePermission(permission)) return@submit

            val name = values[HomeDialogSchema.name]
            val description = values[HomeDialogSchema.description]

            if (home == null) {
                createHome(player, name, description)
            } else {
                editHome(player, home, name, description)
            }
        }

        cancel("Abbrechen") { player ->
            player.playNoSound()

            if (home == null) {
                homeView.open(player)
            } else {
                homeDetailView.open(player, home)
            }
        }
    }

    private fun createHome(player: Player, name: String, description: String) {
        // Read on the callback thread, before the coroutine leaves it
        val location = player.location

        val home = Home(
            ownerUuid = player.uniqueId,
            worldUuid = location.world.uid,
            name = name,
            description = description,
            x = location.x,
            y = location.y,
            z = location.z,
            yaw = location.yaw,
            pitch = location.pitch
        )

        player.launchHomeAction("Das Home konnte nicht erstellt werden.") {
            home.save()

            player.onOwnThread {
                player.playYesSound()
                player.sendText {
                    appendSuccessPrefix()
                    success("Das Home ")
                    variableValue(home.name)
                    success(" wurde erstellt.")
                }

                homeDetailView.open(player, home)
            }
        }
    }

    private fun editHome(player: Player, home: Home, name: String, description: String) {
        player.launchHomeAction("Das Home konnte nicht gespeichert werden.") {
            val edited = home.edit {
                this.name = name
                this.description = description
            }

            player.onOwnThread {
                player.playYesSound()
                player.sendText {
                    appendSuccessPrefix()
                    success("Das Home ")
                    variableValue(edited.name)
                    success(" wurde gespeichert.")
                }

                homeDetailView.open(player, edited)
            }
        }
    }
}
