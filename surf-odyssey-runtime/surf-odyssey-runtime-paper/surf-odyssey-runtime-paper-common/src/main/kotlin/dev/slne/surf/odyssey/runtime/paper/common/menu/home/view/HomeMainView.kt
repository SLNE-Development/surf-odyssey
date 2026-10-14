package dev.slne.surf.odyssey.runtime.paper.common.menu.home.view

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.openForPlayer
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.view.*
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.blockRow
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.paper.inventory.framework.view.icon.viewIcon
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewRows
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.dialog.HomeDialog
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.requirePermission
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry

val homeView: AbstractSurfView = surfView("Homes") {
    settings {
        rows(ViewRows.THREE)
        navigateBackOnOutsideClick(false)
    }

    containerDefaults {
        blockRow(1)
        blockRow(2, exemptColumns = intArrayOf())
        blockRow(3)
    }

    onFirstRender {
        slot(2, 2) {
            withItem(homeListItem)

            onItemClick {
                if (!player.requirePermission(OdysseyBasePermissionRegistry.HOME_LIST)) return@onItemClick

                playGeneralClickSound()
                openForPlayer(homeListView)
            }
        }
        slot(2, 8) {
            withItem(homeCreateItem)

            onItemClick {
                if (!player.requirePermission(OdysseyBasePermissionRegistry.HOME_CREATE)) return@onItemClick

                playGeneralClickSound()
                closeForPlayer()
                player.showDialog(HomeDialog.open())
            }
        }
    }
}

private val homeListItem = viewIcon(ViewIconType.HOME, ViewIconColor.WHITE) {
    displayName {
        success("Homes")
    }

    buildLore {
        emptyLine()
        line {
            spacer("Liste aller Homes".toSmallCaps())
        }
        emptyLine()
    }
}

private val homeCreateItem = viewIcon(ViewIconType.PLUS, ViewIconColor.GREEN) {
    displayName {
        success("Home erstellen")
    }

    buildLore {
        emptyLine()
        line {
            spacer("Erstelle ein neues Home".toSmallCaps())
        }
        emptyLine()
    }
}