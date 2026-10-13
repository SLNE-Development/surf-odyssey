package dev.slne.surf.odyssey.runtime.paper.common.menu.home.dialog

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.runtime.paper.common.dialog.DialogSchema
import dev.slne.surf.odyssey.runtime.paper.common.dialog.inputDialog
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeListView
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import io.papermc.paper.dialog.Dialog

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

object HomeDialog {
    fun open(home: Home? = null): Dialog = inputDialog(HomeDialogSchema) {
        title { primary(if (home == null) "Home erstellen" else "Home bearbeiten") }

        initial(HomeDialogSchema.name, home?.name)
        initial(HomeDialogSchema.description, home?.description)

        submit(if (home == null) "Erstellen" else "Speichern") { player, values ->
            val name = values[HomeDialogSchema.name]
            val description = values[HomeDialogSchema.description]

            plugin.launch {
                if (home != null) {
                    home.edit {
                        this.name = name
                        this.description = description
                    }
                } else {
                    val location = player.location

                    Home(
                        ownerUuid = player.uniqueId,
                        worldUuid = location.world.uid,
                        name = name,
                        description = description,
                        x = location.x,
                        y = location.y,
                        z = location.z,
                        yaw = location.yaw,
                        pitch = location.pitch
                    ).save()
                }

                player.playYesSound()

                // SHOW HOME VIEW
            }
        }

        cancel("Abbrechen") { player ->
            player.playNoSound()
            homeListView.open(player)
        }
    }
}
