@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.trade

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemRender
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
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.closeItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMode
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.Price
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeOffer
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeResult
import dev.slne.surf.odyssey.runtime.paper.progression.operation.PlayerEscrowInventory
import dev.slne.surf.odyssey.runtime.paper.progression.station.CraftPlanner
import dev.slne.surf.odyssey.runtime.paper.progression.station.RecipeBrowserView
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * The trader menu of one NPC, passed as the initial data: one item per offer of the NPC in definition order. An
 * offer shows what it sells, its price in green where the player carries enough and in red where not, and its state.
 * Clicking an affordable offer buys it and renders the current page again if the player still looks at it.
 */
val tradeView: AbstractPaginatedSurfView = paginatedSurfView("Händler") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
    }

    layoutTarget('T')

    pagination<TradeOffer> {
        lazySource { context ->
            val npc = context.initialData as? NpcDefinition
            npc?.trades ?: emptyList()
        }
        itemFactory { offer ->
            onItemRender {
                item = TradeView.itemOf(player, offer)
            }
            onItemClick {
                playGeneralClickSound()
                val menu = parent
                Trading.buy(player, offer) { buyer -> RecipeBrowserView.refreshIfShown(menu, buyer) }
            }
        }
    }

    onFirstRender {
        slot(4, 1) {
            withItem(closeItem)
            onItemClick {
                playGeneralClickSound()
                closeForPlayer()
            }
        }
    }
}

/** Opens the trader menu and builds its offer items. */
object TradeView {
    /**
     * Opens the trader menu of [npc] for [player], if trading works on this server and the player's progression is
     * loaded.
     */
    fun open(player: Player, npc: NpcDefinition) {
        val runtime = Progression.runtimeOrNull ?: return
        if (runtime.mode != ProgressionMode.FULL) return
        if (runtime.store[player.uniqueId] == null) {
            ProgressionMessages.unavailable(player)
            return
        }
        tradeView.open(player, npc)
    }

    /** Returns how much of each ingredient of the item price of [offer] [player] carries, by [CraftPlanner.countKey]. */
    fun inventoryCounts(player: Player, offer: TradeOffer): Map<String, Int> {
        val price = offer.price as? Price.Items ?: return emptyMap()
        val inventory = PlayerEscrowInventory(player)
        return price.ingredients.associate { CraftPlanner.countKey(it.item) to inventory.count(it.item) }
    }

    /**
     * Builds the item shown for [offer] to [player]: the sold item or the result item of a sold recipe named
     * `Rezept: <Name>`, with the price lines and the state of the offer as lore.
     */
    fun itemOf(player: Player, offer: TradeOffer): ItemStack {
        val counts = inventoryCounts(player, offer)
        val progression = Progression.runtimeOrNull?.store?.get(player.uniqueId)
        val state = progression?.let { TradeOfferState.of(offer, it, counts) } ?: TradeOfferState.TOO_EXPENSIVE
        val stack = when (val result = offer.result) {
            is TradeResult.Item -> result.item.create(1).also { it.amount = result.amount.coerceIn(1, it.maxStackSize) }
            is TradeResult.Recipe -> result.recipe.result.create(1)
        }
        if (offer.result is TradeResult.Recipe) {
            val name = Trading.nameOf(offer)
            stack.editMeta { it.itemName(name) }
        }
        stack.buildLore {
            emptyLine()
            line { info("Preis:") }
            when (val price = offer.price) {
                is Price.Items -> price.ingredients.forEach { ingredient ->
                    val enough = (counts[CraftPlanner.countKey(ingredient.item)] ?: 0) >=
                        (TradeOfferState.needed(price)[CraftPlanner.countKey(ingredient.item)] ?: 0)
                    line {
                        if (enough) success("${ingredient.amount}× ") else error("${ingredient.amount}× ")
                        append(ingredient.item.displayName.color(if (enough) Colors.SUCCESS else Colors.ERROR))
                    }
                }
                is Price.Currency -> line { variableValue("${price.amount} ${price.currencyId}") }
            }
            emptyLine()
            when (state) {
                TradeOfferState.KNOWN -> line { spacer("Bekannt".toSmallCaps()) }
                TradeOfferState.CURRENCY_UNAVAILABLE -> line { spacer("Bald verfügbar".toSmallCaps()) }
                TradeOfferState.AFFORDABLE -> line { spacer("Klick: kaufen".toSmallCaps()) }
                TradeOfferState.TOO_EXPENSIVE -> Unit
            }
        }
        return stack
    }
}
