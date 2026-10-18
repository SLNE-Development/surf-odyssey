@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.builder.meta
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemRender
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.onClose
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeManager
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeRequest
import dev.slne.surf.odyssey.runtime.paper.common.trade.acceptTradeRequest
import dev.slne.surf.odyssey.runtime.paper.common.trade.declineTradeRequest
import dev.slne.surf.odyssey.runtime.paper.common.trade.withdrawTradeRequest
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import me.devnatan.inventoryframework.context.Context
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

/**
 * Lists the open trade requests of the viewer, received ones first. A left click accepts a received request, a right
 * click declines it and a click on a sent request withdraws it.
 */
val tradeListView: AbstractPaginatedSurfView = paginatedSurfView("Handelsanfragen") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
    }

    layoutTarget('H')

    pagination<TradeRequest> {
        computedSource { context -> TradeManager.requestsOf(context.player) }
        elementFactory { context, builder, _, request ->
            val viewer = context.player
            val received = request.receiver == viewer.uniqueId

            with(builder) {
                onItemRender { item = requestItem(request, received) }

                onItemClick {
                    playGeneralClickSound()

                    if (received) {
                        val sender = viewer.onlinePartner(request.sender) ?: return@onItemClick

                        if (isRightClick) {
                            viewer.declineTradeRequest(sender)
                        } else {
                            // The trade window opens on the next tick, after this menu is closed
                            closeForPlayer()
                            viewer.scheduler.run(plugin, { viewer.acceptTradeRequest(sender) }, null)
                            return@onItemClick
                        }
                    } else {
                        viewer.onlinePartner(request.receiver)?.let { viewer.withdrawTradeRequest(it) }
                    }

                    reopen(viewer, this)
                }
            }
        }
    }

    onFirstRender {
        val render = this
        val viewer = player
        val shown = TradeManager.requestsOf(viewer).map { it.id }

        refreshTasks.remove(viewer.uniqueId)?.cancel()
        viewer.scheduler.runAtFixedRate(
            plugin,
            { task ->
                when {
                    TradeManager.isTrading(viewer.uniqueId) -> task.cancel()
                    TradeManager.requestsOf(viewer).map { it.id } != shown -> reopen(viewer, render)
                    else -> render.update()
                }
            },
            null,
            REFRESH_TICKS,
            REFRESH_TICKS
        )?.let { refreshTasks[viewer.uniqueId] = it }
    }

    onClose {
        refreshTasks.remove(player.uniqueId)?.cancel()
    }
}

private const val REFRESH_TICKS = 20L

/** The tasks that refresh the remaining times of the open list views, by viewer. */
private val refreshTasks = ConcurrentHashMap<UUID, ScheduledTask>()

/**
 * Closes the list of [viewer] and, if they still have open requests, opens it again on the next tick so it shows
 * the current requests.
 */
private fun reopen(viewer: Player, context: Context) {
    context.closeForPlayer()

    if (TradeManager.requestsOf(viewer).isEmpty()) return

    viewer.scheduler.run(plugin, { tradeListView.open(viewer) }, null)
}

/** The player with [uuid] if they are online, otherwise tells this player the other player left and returns `null`. */
private fun Player.onlinePartner(uuid: UUID): Player? {
    val partner = Bukkit.getPlayer(uuid)

    if (partner == null) {
        playNoSound()
        sendText {
            appendErrorPrefix()
            error("Der Spieler ist nicht mehr online.")
        }
    }

    return partner
}

/** The head of the other player of [request] with its direction, remaining time and click hints. */
private fun requestItem(request: TradeRequest, received: Boolean): ItemStack {
    val partnerUuid = if (received) request.sender else request.receiver
    val partnerName = if (received) request.senderName else request.receiverName
    val seconds = ceil((request.expiresAt - System.currentTimeMillis()) / 1000.0).toInt().coerceAtLeast(1)

    return buildItem(Material.PLAYER_HEAD) {
        meta<SkullMeta> { owningPlayer = Bukkit.getOfflinePlayer(partnerUuid) }
        displayName { primary(partnerName) }

        buildLore {
            emptyLine()
            line {
                if (received) info("Eingehende Anfrage") else info("Ausgehende Anfrage")
            }
            line {
                info("Läuft ab in ")
                variableValue("${seconds}s")
            }
            emptyLine()

            if (received) {
                line { spacer("Linksklick: Annehmen".toSmallCaps()) }
                line { spacer("Rechtsklick: Ablehnen".toSmallCaps()) }
            } else {
                line { spacer("Klicke, um die Anfrage zurückzuziehen".toSmallCaps()) }
            }

            emptyLine()
        }
    }
}
