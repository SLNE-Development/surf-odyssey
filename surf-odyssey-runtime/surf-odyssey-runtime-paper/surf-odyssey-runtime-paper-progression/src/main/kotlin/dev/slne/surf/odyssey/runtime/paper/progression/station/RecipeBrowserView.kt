@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemRender
import dev.slne.surf.api.paper.inventory.framework.dsl.openForPlayer
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.dsl.withItem
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.backItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.operation.PlayerEscrowInventory
import me.devnatan.inventoryframework.context.IFRenderContext
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * The recipe browser of one station kind, passed as the initial data: every recipe of the station in registry order.
 * Unlocked recipes show the result item with ingredients and crafting time, locked recipes a gray dye named `???`.
 * Clicking an unlocked recipe crafts it once, shift-clicking an instant recipe as often as possible; afterwards the
 * current page is rendered again if the player still looks at it. The back button opens the station menu.
 */
val recipeBrowserView: AbstractPaginatedSurfView = paginatedSurfView("Rezepte") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
    }

    layoutTarget('R')

    pagination<RecipeDefinition> {
        lazySource { context ->
            val station = context.initialData as? StationDefinition
            if (station == null) emptyList() else Progression.runtime.registry.recipesOf(station)
        }
        itemFactory { recipe ->
            onItemRender {
                item = RecipeBrowserView.itemOf(RecipeBrowserView.iconFor(player, recipe), recipe)
            }
            onItemClick {
                playGeneralClickSound()
                val browser = parent
                Crafting.craft(player, recipe, everything = isShiftClick) { crafter ->
                    RecipeBrowserView.refreshIfShown(browser, crafter)
                }
            }
        }
    }

    onFirstRender {
        slot(4, 1) {
            withItem(backItem)
            onItemClick {
                playGeneralClickSound()
                val station = initialData as? StationDefinition
                if (station == null) closeForPlayer() else openForPlayer(stationView, station)
            }
        }
    }
}

/** Opens the recipe browser and builds its recipe items. */
object RecipeBrowserView {
    /**
     * Opens the recipe browser of [station] for [player], if stations can be used on this server and the player's
     * progression is loaded.
     */
    fun open(player: Player, station: StationDefinition) {
        if (!StationView.canUse(player)) return
        recipeBrowserView.open(player, station)
    }

    /**
     * Renders the current page of the browser [context] again if it is still the menu [player] looks at; does
     * nothing after the player closed it or switched to another menu.
     */
    fun refreshIfShown(context: IFRenderContext, player: Player) {
        val id = player.uniqueId.toString()
        if (context.viewers.any { it.id == id && it.activeContext === context }) context.update()
    }

    /**
     * Describes [recipe] for [player]: unlocked when the recipe is unlocked by default or in the player's progression,
     * with the ingredient counts of the player's inventory.
     */
    fun iconFor(player: Player, recipe: RecipeDefinition): RecipeIcon {
        val progression = Progression.runtimeOrNull?.store?.get(player.uniqueId)
        val unlocked = recipe.unlockedByDefault || progression?.isRecipeUnlocked(recipe.id) == true
        return if (unlocked) RecipeIcon.unlocked(recipe, inventoryCounts(player, recipe)) else RecipeIcon.locked(recipe)
    }

    /**
     * Returns how much of each ingredient of [recipe] [player] carries, by [CraftPlanner.countKey].
     */
    fun inventoryCounts(player: Player, recipe: RecipeDefinition): Map<String, Int> {
        val inventory = PlayerEscrowInventory(player)
        return recipe.ingredients.associate { CraftPlanner.countKey(it.item) to inventory.count(it.item) }
    }

    /**
     * Builds the item shown for [icon]: the result of [recipe] for an unlocked icon, with the icon amount capped at
     * the item's maximum stack size, or a gray named item otherwise, with the styled lore lines of the icon.
     */
    fun itemOf(icon: RecipeIcon, recipe: RecipeDefinition): ItemStack {
        val stack = if (icon.unlocked) {
            recipe.result.create(1).also { it.amount = icon.amount.coerceIn(1, it.maxStackSize) }
        } else {
            buildItem(icon.material, icon.amount) { displayName { gray(icon.name) } }
        }
        stack.buildLore {
            icon.lore.forEach { line ->
                when (line) {
                    is RecipeLoreLine.IngredientLine -> line {
                        if (line.enough) success("${line.amount}× ") else error("${line.amount}× ")
                        append(line.name.color(if (line.enough) Colors.SUCCESS else Colors.ERROR))
                    }
                    is RecipeLoreLine.Plain -> when (line.kind) {
                        RecipeLoreLine.LineKind.EMPTY -> emptyLine()
                        RecipeLoreLine.LineKind.HEADING -> line { info(line.text) }
                        RecipeLoreLine.LineKind.VALUE -> line { variableValue(line.text) }
                        RecipeLoreLine.LineKind.HINT -> line { spacer(line.text.toSmallCaps()) }
                        RecipeLoreLine.LineKind.LOCKED -> line { gray(line.text) }
                    }
                }
            }
        }
        return stack
    }
}
