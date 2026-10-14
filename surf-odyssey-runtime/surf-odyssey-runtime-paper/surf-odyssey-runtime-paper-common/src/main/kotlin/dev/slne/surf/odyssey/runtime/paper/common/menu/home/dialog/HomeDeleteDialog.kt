@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.common.menu.home.dialog

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.launchHomeAction
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.onOwnThread
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.requirePermission
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeDetailView
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeListView
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry
import io.papermc.paper.dialog.Dialog

/** Asks the player to confirm deleting a home before it is deleted. */
object HomeDeleteDialog {
    fun open(home: Home): Dialog = dialog {
        base {
            title { error("Home löschen") }
            body {
                plainMessage {
                    text("Möchtest du das Home ")
                    variableValue(home.name)
                    text(" wirklich löschen? Das kann nicht rückgängig gemacht werden.")
                }
            }
        }

        type {
            confirmation {
                yes {
                    label { error("Löschen") }
                    action {
                        playerCallback { player ->
                            if (!player.requirePermission(OdysseyBasePermissionRegistry.HOME_DELETE)) {
                                return@playerCallback
                            }

                            player.launchHomeAction("Das Home konnte nicht gelöscht werden.") {
                                home.delete()

                                player.onOwnThread {
                                    player.playYesSound()
                                    player.sendText {
                                        appendSuccessPrefix()
                                        success("Das Home ")
                                        variableValue(home.name)
                                        success(" wurde gelöscht.")
                                    }

                                    homeListView.open(player)
                                }
                            }
                        }
                    }
                }

                no {
                    label { text("Abbrechen") }
                    action {
                        playerCallback { player ->
                            player.playNoSound()
                            homeDetailView.open(player, home)
                        }
                    }
                }
            }
        }
    }
}
