@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.common.menu.home.view

import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.openForPlayer
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.createHomeItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.backItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.common.player.cachedPlayerOrFail

val homeListView: AbstractPaginatedSurfView = paginatedSurfView("Meine Homes") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
    }

    layoutTarget('H')

    pagination {
        lazySource { context -> context.player.cachedPlayerOrFail().homes.sortedBy { it.name.lowercase() } }
        itemFactory { home ->
            withItem(createHomeItem(home, "Klicke, um das Home zu öffnen"))

            onItemClick {
                playGeneralClickSound()
                openForPlayer(homeDetailView, home)
            }
        }
    }

    onFirstRender {
        slot(4, 1, backItem).onClick { click ->
            playGeneralClickSound()

            click.openForPlayer(homeView)
        }
    }
}
