package dev.slne.surf.odyssey.runtime.paper.common.menu.home.view

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.openForPlayer
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.view.AbstractSurfView
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.blockRow
import dev.slne.surf.api.paper.inventory.framework.view.containerDefaults
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.paper.inventory.framework.view.icon.viewIcon
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewRows
import dev.slne.surf.api.paper.inventory.framework.view.state.get
import dev.slne.surf.api.paper.inventory.framework.view.state.initialState
import dev.slne.surf.api.paper.inventory.framework.view.surfView
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.createHomeItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.dialog.HomeDeleteDialog
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.dialog.HomeDialog
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.requirePermission
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.teleportToHome
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.backItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.common.player.cachedPlayer
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry

/** Shows one home, passed as the view's initial data, with teleport, edit and delete actions. */
val homeDetailView: AbstractSurfView = surfView("Home") {
    val homeState = initialState<Home>()

    settings {
        rows(ViewRows.THREE)
    }

    containerDefaults {
        blockRow(1)
        blockRow(2, exemptColumns = intArrayOf())
        blockRow(3)
    }

    onFirstRender {
        // Prefer the cached home, so a view reopened from an older menu shows the latest name and description
        val initialHome = homeState[this]
        val home = player.cachedPlayer()?.home(initialHome.id) ?: initialHome

        slot(2, 1) {
            withItem(backItem)

            onItemClick {
                playGeneralClickSound()
                openForPlayer(homeListView)
            }
        }

        slot(2, 3) {
            withItem(teleportItem)

            onItemClick {
                playGeneralClickSound()
                closeForPlayer()
                player.teleportToHome(home)
            }
        }

        slot(2, 5) {
            withItem(createHomeItem(home))
        }

        slot(2, 7) {
            withItem(editItem)

            onItemClick {
                if (!player.requirePermission(OdysseyBasePermissionRegistry.HOME_EDIT)) return@onItemClick

                playGeneralClickSound()
                closeForPlayer()
                player.showDialog(HomeDialog.open(home))
            }
        }

        slot(2, 9) {
            withItem(deleteItem)

            onItemClick {
                if (!player.requirePermission(OdysseyBasePermissionRegistry.HOME_DELETE)) return@onItemClick

                playGeneralClickSound()
                closeForPlayer()
                player.showDialog(HomeDeleteDialog.open(home))
            }
        }
    }
}

private val teleportItem = viewIcon(ViewIconType.ARROW_RIGHT, ViewIconColor.GREEN) {
    displayName {
        success("Teleportieren")
    }

    buildLore {
        emptyLine()
        line {
            spacer("Zu diesem Home teleportieren".toSmallCaps())
        }
        emptyLine()
    }
}

private val editItem = viewIcon(ViewIconType.COG, ViewIconColor.YELLOW) {
    displayName {
        primary("Bearbeiten")
    }

    buildLore {
        emptyLine()
        line {
            spacer("Name und Beschreibung ändern".toSmallCaps())
        }
        emptyLine()
    }
}

private val deleteItem = viewIcon(ViewIconType.CROSS, ViewIconColor.RED) {
    displayName {
        error("Löschen")
    }

    buildLore {
        emptyLine()
        line {
            spacer("Dieses Home löschen".toSmallCaps())
        }
        emptyLine()
    }
}
