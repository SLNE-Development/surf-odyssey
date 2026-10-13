package dev.slne.surf.odyssey.runtime.paper.common.menu.home.dialog

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeListView
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import io.papermc.paper.dialog.Dialog

private const val NAME_INPUT = "name"
private const val DESCRIPTION_INPUT = "description"

object HomeDialog {
    fun open(
        home: Home? = null,
        name: String? = home?.name,
        description: String? = home?.description,
    ): Dialog = dialog {
        base {
            title {
                primary("Home erstellen")

                body {
                    input {
                        text(NAME_INPUT) {
                            labelVisible(true)
                            label {
                                text("Name")
                            }

                            initial(name ?: "")
                            maxLength(16)
                        }
                    }

                    input {
                        text(DESCRIPTION_INPUT) {
                            labelVisible(true)
                            label {
                                text("Beschreibung")
                            }

                            initial(description ?: "")
                            maxLength(500)
                            multiline(10, 300)
                        }
                    }
                }
            }

            type {
                confirmation {
                    yes {
                        label {
                            success("Erstellen")
                        }

                        action {
                            customPlayerClick { response, player ->
                                val nameInput = response.getText(NAME_INPUT) ?: ""
                                val descriptionInput = response.getText(DESCRIPTION_INPUT) ?: ""

                                val normalizedName = normalizeName(nameInput)
                                val normalizedDescription = normalizeDescription(descriptionInput)

                                if (!normalizedName.state || !normalizedDescription.state) {
                                    player.showDialog(
                                        HomeErrorDialog.create(
                                            name = normalizedName,
                                            description = normalizedDescription,
                                            home = home
                                        )
                                    )

                                    return@customPlayerClick
                                }

                                plugin.launch {
                                    if (home != null) {
                                        home.edit {
                                            this.name = normalizedName.value
                                            this.description = normalizedDescription.value
                                        }
                                    } else {
                                        val location = player.location

                                        val newHome = Home(
                                            ownerUuid = player.uniqueId,
                                            worldUuid = location.world.uid,
                                            name = normalizedName.value,
                                            description = normalizedDescription.value,
                                            x = location.x,
                                            y = location.y,
                                            z = location.z,
                                            yaw = location.yaw,
                                            pitch = location.pitch
                                        )

                                        newHome.save()
                                    }

                                    player.playYesSound()

                                    // SHOW HOME VIEW
                                }
                            }
                        }
                    }

                    no {
                        label {
                            text("Abbrechen")
                        }

                        action {
                            playerCallback { player ->
                                player.playNoSound()
                                homeListView.open(player)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun normalizeName(name: String): HomeDialogInputState {
        val trimmedName = name.trim()

        if (trimmedName.isEmpty()) {
            return HomeDialogInputState(
                state = false,
                value = trimmedName,
                error = "Der Name darf nicht leer sein."
            )
        }

        if (trimmedName.length > 16) {
            return HomeDialogInputState(
                state = false,
                value = trimmedName,
                error = "Der Name darf maximal 16 Zeichen lang sein."
            )
        }

        return HomeDialogInputState(
            state = true,
            value = trimmedName
        )
    }

    private fun normalizeDescription(description: String): HomeDialogInputState {
        val trimmedDescription = description.trim()

        if (trimmedDescription.length > 500) {
            return HomeDialogInputState(
                state = false,
                value = trimmedDescription,
                error = "Die Beschreibung darf maximal 500 Zeichen lang sein."
            )
        }

        return HomeDialogInputState(
            state = true,
            value = trimmedDescription
        )
    }
}