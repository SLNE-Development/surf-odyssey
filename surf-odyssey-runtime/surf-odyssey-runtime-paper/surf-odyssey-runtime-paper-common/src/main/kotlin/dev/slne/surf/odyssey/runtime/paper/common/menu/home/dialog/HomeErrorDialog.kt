package dev.slne.surf.odyssey.runtime.paper.common.menu.home.dialog

import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.odyssey.core.common.home.Home
import io.papermc.paper.dialog.Dialog

object HomeErrorDialog {
    fun create(
        name: HomeDialogInputState,
        description: HomeDialogInputState,
        home: Home? = null
    ): Dialog = dialog {
        base {
            title {
                error("Home erstellen - Fehler")
            }

            body {
                plainMessage {
                    error("Es ist ein Fehler aufgetreten. Bitte überprüfe deine Eingaben und versuche es erneut.")
                }
            }
        }

        type {
            notice {
                action {
                    label {
                        text("Zurück")
                    }

                    playerCallback { player ->
                        player.showDialog(
                            HomeDialog.open(
                                home = home,
                                name = name.value,
                                description = description.value,
                            )
                        )
                    }
                }
            }
        }
    }
}