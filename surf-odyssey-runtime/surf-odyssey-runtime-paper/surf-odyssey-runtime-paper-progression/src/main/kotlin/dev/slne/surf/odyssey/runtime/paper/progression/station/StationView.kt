package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.openForPlayer
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.dsl.withItem
import dev.slne.surf.api.paper.inventory.framework.modifyConfig
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.api.paper.inventory.framework.titleBuilder
import dev.slne.surf.api.paper.inventory.framework.view.AbstractSurfView
import dev.slne.surf.api.paper.inventory.framework.view.containerDefaults
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.blockRow
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.onOpen
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewRows
import dev.slne.surf.api.paper.inventory.framework.view.state.get
import dev.slne.surf.api.paper.inventory.framework.view.state.initialState
import dev.slne.surf.api.paper.inventory.framework.view.surfView
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.closeItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMode
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * The menu of one station kind, passed as the initial data and named after it. The button in the bottom row opens
 * the recipe browser of the station.
 */
val stationView: AbstractSurfView = surfView("Station") {
    val stationState = initialState<StationDefinition>()

    settings {
        rows(ViewRows.THREE)
        navigateBackOnOutsideClick(false)
    }

    containerDefaults {
        blockRow(3)
    }

    onOpen {
        val station = stationState[this]
        modifyConfig { titleBuilder { append(station.displayName) } }
    }

    onFirstRender {
        val station = stationState[this]

        slot(3, 1) {
            withItem(closeItem)
            onItemClick {
                playGeneralClickSound()
                closeForPlayer()
            }
        }

        slot(3, 5) {
            withItem(recipesItem)
            onItemClick {
                playGeneralClickSound()
                openForPlayer(recipeBrowserView, station)
            }
        }
    }
}

/** The button that opens the recipe browser. */
private val recipesItem = buildItem(Material.BOOK) {
    displayName { primary("Rezepte") }
    buildLore {
        emptyLine()
        line { spacer("Alle Rezepte dieser Station".toSmallCaps()) }
        emptyLine()
    }
}

/** Opens station menus. */
object StationView {
    /**
     * Opens the menu of [station] for [player]. Nothing opens on servers that do not run
     * [ProgressionMode.FULL]; while the player's progression is not available the player is told so.
     */
    fun open(player: Player, station: StationDefinition) {
        if (!canUse(player)) return
        stationView.open(player, station)
    }

    /**
     * Returns whether [player] can use stations now: the engine runs [ProgressionMode.FULL] and the player's
     * progression is available. When only the progression is missing, the player is told that stations are not
     * available.
     */
    fun canUse(player: Player): Boolean {
        val runtime = Progression.runtimeOrNull ?: return false
        if (runtime.mode != ProgressionMode.FULL) return false
        if (!runtime.store.isAvailableFor(player.uniqueId)) {
            ProgressionMessages.unavailable(player)
            return false
        }
        return true
    }
}
