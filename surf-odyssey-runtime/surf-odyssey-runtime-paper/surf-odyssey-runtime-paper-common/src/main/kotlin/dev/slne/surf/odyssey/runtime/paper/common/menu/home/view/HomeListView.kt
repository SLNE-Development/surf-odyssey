@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.common.menu.home.view

import dev.slne.surf.api.paper.builder.LoreBuilder
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.backItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.common.player.cachedPlayerOrFail
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

val homeListView = paginatedSurfView("Meine Homes") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
    }

    layoutTarget('H')

    pagination {
        lazySource { it.player.cachedPlayerOrFail().homes }
        itemFactory {
            withItem(createHomeItem(it))

            onItemClick {
                playGeneralClickSound()

                // Open the home details view for the clicked home
            }
        }
    }

    onFirstRender {
        slot(4, 1, backItem).onClick { click ->
            playGeneralClickSound()

            click.openForPlayer(
                homeView::class.java
            )
        }
    }
}

private fun createHomeItem(home: Home): ItemStack {
    val registryItemType = Registry.ITEM[home.itemType] ?: ItemType.NAME_TAG

    return buildItem(registryItemType) {
        displayName {
            primary(home.name)
        }

        buildLore {
            if (home.description.isNotEmpty()) {
                emptyLine()
                appendHomeDescription(description = home.description)
            }

            emptyLine()
            line {
                spacer("Klicke, um mehr Informationen über ")
            }
            line {
                primary(home.name)
                spacer(" zu erhalten.")
            }
            emptyLine()
        }
    }
}

private fun LoreBuilder.appendHomeDescription(maxWords: Int = 8, description: String) {
    val words = description.split(" ")
    val lines = words.chunked(maxWords).map { it.joinToString(" ") }

    lines.forEach { line ->
        line {
            spacer(line)
        }
    }
}