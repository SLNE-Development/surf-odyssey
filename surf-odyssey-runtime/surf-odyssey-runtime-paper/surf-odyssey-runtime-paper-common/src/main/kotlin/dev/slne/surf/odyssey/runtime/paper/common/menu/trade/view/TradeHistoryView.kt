@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.paper.builder.LoreBuilder
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.builder.meta
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.withItem
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import dev.slne.surf.odyssey.core.common.trade.TradeCancelType
import dev.slne.surf.odyssey.core.common.trade.TradeOutcome
import dev.slne.surf.odyssey.core.common.trade.TradeRecord
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val MAX_LISTED_ITEMS = 5

private val timeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Berlin"))

/** The trades each viewer is shown, set before the history view opens and kept until they leave the server. */
private val shownTrades = ConcurrentHashMap<UUID, List<TradeRecord>>()

/** Forgets the trade history shown to [uuid]. */
internal fun forgetTradeHistory(uuid: UUID) {
    shownTrades.remove(uuid)
}

/** Opens the trade history last shown to [viewer] again, if there is one. */
internal fun reopenTradeHistory(viewer: Player) {
    shownTrades[viewer.uniqueId]?.let { openTradeHistory(viewer, it) }
}

/** Formats [millis] as the date and time shown in the trade menus. */
internal fun formatTradeTime(millis: Long): String = timeFormat.format(Instant.ofEpochMilli(millis))

/**
 * Shows [trades], newest first, in the trade history of this player. Must run on the thread that owns the player, as
 * the menu opens right away.
 */
internal fun openTradeHistory(viewer: Player, trades: List<TradeRecord>) {
    shownTrades[viewer.uniqueId] = trades
    tradeHistoryView.open(viewer)
}

/** Lists the finished trades of the viewer with the partner, the outcome, the time and the items of both sides. */
val tradeHistoryView: AbstractPaginatedSurfView = paginatedSurfView("Handelsverlauf") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
    }

    layoutTarget('H')

    pagination<TradeRecord> {
        computedSource { context -> shownTrades[context.player.uniqueId].orEmpty() }
        elementFactory { context, builder, _, trade ->
            val viewer = context.player

            with(builder) {
                withItem(tradeItem(trade, viewer.uniqueId))

                onItemClick {
                    playGeneralClickSound()

                    // The detail menu opens on the next tick, after this menu is closed
                    closeForPlayer()
                    viewer.scheduler.run(plugin, { openTradeDetail(viewer, trade) }, null)
                }
            }
        }
    }
}

/** The head of the partner of [viewer] in [trade] with the outcome, the time and both offers. */
private fun tradeItem(trade: TradeRecord, viewer: UUID): ItemStack {
    val own = trade.firstUuid == viewer
    val partnerUuid = if (own) trade.secondUuid else trade.firstUuid
    val partnerName = Bukkit.getOfflinePlayer(partnerUuid).name ?: "Unbekannt"
    val completed = trade.outcome == TradeOutcome.COMPLETED

    val ownItems = if (own) trade.firstItems else trade.secondItems
    val partnerItems = if (own) trade.secondItems else trade.firstItems

    return buildItem(Material.PLAYER_HEAD) {
        meta<SkullMeta> { owningPlayer = Bukkit.getOfflinePlayer(partnerUuid) }
        displayName { primary(partnerName) }

        buildLore {
            emptyLine()
            line {
                if (completed) success("Abgeschlossen") else error(cancelText(trade, viewer))
            }
            line { info(formatTradeTime(trade.endedAtMillis)) }
            emptyLine()

            line { spacer((if (completed) "Du hast gegeben" else "Dein Angebot").toSmallCaps()) }
            appendItems(ownItems)
            emptyLine()

            line { spacer((if (completed) "Du hast erhalten" else "Angebot des Partners").toSmallCaps()) }
            appendItems(partnerItems)
            emptyLine()

            line { spacer("Klicke für alle Items".toSmallCaps()) }
            emptyLine()
        }
    }
}

/** The reason [trade] was cancelled as shown to [viewer]. */
private fun cancelText(trade: TradeRecord, viewer: UUID) = when {
    trade.cancelType == TradeCancelType.SHUTDOWN -> "Abgebrochen (Serverneustart)"
    trade.cancelType == TradeCancelType.LEFT -> "Abgebrochen (Spieler verlassen)"
    trade.cancelledBy == viewer -> "Von dir abgebrochen"
    else -> "Vom Partner abgebrochen"
}

/** Adds one line per item of [encoded], at most [MAX_LISTED_ITEMS], or a note that nothing was offered. */
private fun LoreBuilder.appendItems(encoded: List<String>) {
    val items = encoded.mapNotNull(::decode)

    if (items.isEmpty()) {
        line { info("Nichts") }
        return
    }

    items.take(MAX_LISTED_ITEMS).forEach { item ->
        line {
            variableValue("${item.amount}x")
            appendSpace()
            info(PlainTextComponentSerializer.plainText().serialize(item.effectiveName()))
        }
    }

    if (items.size > MAX_LISTED_ITEMS) {
        line { spacer("und ${items.size - MAX_LISTED_ITEMS} weitere".toSmallCaps()) }
    }
}

/** Decodes the stored item stacks, skipping any that cannot be read. */
internal fun decodeTradeItems(encoded: List<String>): List<ItemStack> = encoded.mapNotNull(::decode)

/** The item stack stored in [encoded], or `null` if it cannot be read. */
private fun decode(encoded: String): ItemStack? =
    runCatching { ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded)) }.getOrNull()
