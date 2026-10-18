@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.onClose
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.odyssey.core.common.trade.TradeOutcome
import dev.slne.surf.odyssey.core.common.trade.TradeRecord
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.backItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** The trade each viewer is shown in detail, set before the detail view opens and removed when it closes. */
private val shownDetails = ConcurrentHashMap<UUID, TradeRecord>()

/** Opens the detail view of [trade] for [viewer]. Must run on the thread that owns the player. */
internal fun openTradeDetail(viewer: Player, trade: TradeRecord) {
    shownDetails[viewer.uniqueId] = trade
    tradeDetailView.open(viewer)
}

/**
 * Lists every item of one trade as the real item, marked with the side it came from. The viewer's own items come
 * first, then the items of the partner.
 */
val tradeDetailView: AbstractPaginatedSurfView = paginatedSurfView("Handel-Details") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
    }

    layoutTarget('H')

    pagination<ItemStack> {
        computedSource { context -> detailItems(shownDetails[context.player.uniqueId], context.player.uniqueId) }
        elementFactory { _, builder, _, item ->
            builder.withItem(item)
        }
    }

    onFirstRender {
        val viewer = player

        slot(4, 1, backItem).onClick { click ->
            playGeneralClickSound()

            // The history opens on the next tick, after this menu is closed
            click.closeForPlayer()
            viewer.scheduler.run(plugin, { reopenTradeHistory(viewer) }, null)
        }
    }

    onClose {
        shownDetails.remove(player.uniqueId)
    }
}

/** The items of [trade] as shown to [viewer], each with a lore line naming the side it belongs to. */
private fun detailItems(trade: TradeRecord?, viewer: UUID): List<ItemStack> {
    trade ?: return emptyList()

    val own = trade.firstUuid == viewer
    val completed = trade.outcome == TradeOutcome.COMPLETED

    val ownItems = decodeTradeItems(if (own) trade.firstItems else trade.secondItems)
    val partnerItems = decodeTradeItems(if (own) trade.secondItems else trade.firstItems)

    val ownLabel =
        if (completed) buildText { error("Du hast gegeben".toSmallCaps()) } else buildText { error("Dein Angebot".toSmallCaps()) }
    val partnerLabel =
        if (completed) buildText { success("Du hast erhalten".toSmallCaps()) } else buildText { success("Angebot des Partners".toSmallCaps()) }

    return ownItems.map { it.labelled(ownLabel) } + partnerItems.map { it.labelled(partnerLabel) }
}

/** A copy of this item with [label] added to the end of its lore. */
private fun ItemStack.labelled(label: Component): ItemStack = clone().also { copy ->
    copy.editMeta { meta ->
        meta.lore((meta.lore() ?: emptyList()) + listOf(Component.empty(), label))
    }
}
