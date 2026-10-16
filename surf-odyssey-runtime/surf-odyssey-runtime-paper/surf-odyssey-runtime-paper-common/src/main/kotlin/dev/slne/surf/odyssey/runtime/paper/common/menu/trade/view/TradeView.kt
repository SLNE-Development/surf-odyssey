package dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.builder.meta
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemRender
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.view.AbstractSurfView
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.paper.inventory.framework.view.icon.viewIcon
import dev.slne.surf.api.paper.inventory.framework.modifyConfig
import dev.slne.surf.api.paper.inventory.framework.titleBuilder
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.blockColumn
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.blockRow
import dev.slne.surf.api.paper.inventory.framework.view.containerDefaults
import dev.slne.surf.api.paper.inventory.framework.view.onClose
import dev.slne.surf.api.paper.inventory.framework.view.onOpen
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewRows
import dev.slne.surf.api.paper.inventory.framework.view.surfView
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import dev.slne.surf.odyssey.runtime.paper.common.trade.TRADE_OFFER_SLOTS
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeCancelReason
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeManager
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradePhase
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeSession
import dev.slne.surf.odyssey.runtime.paper.common.trade.hasRoomFor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import kotlin.math.ceil

private const val GRID_SIZE = 4

private const val PARTNER_COLUMN_OFFSET = 5

private const val REFRESH_TICKS = 5L

val tradeView: AbstractSurfView = surfView("Handel") {
    settings {
        rows(ViewRows.SIX)
        navigateBackOnOutsideClick(false)
    }

    containerDefaults {
        blockColumn(GRID_SIZE, intArrayOf(GRID_SIZE + 2))
        blockRow(GRID_SIZE + 1, exemptColumns = intArrayOf(1, 7))
        blockRow(GRID_SIZE + 2, exemptColumns = intArrayOf(0, 3, 4, 5, 8))
    }

    onOpen {
        val partner = TradeManager.sessionOf(player.uniqueId)?.partnerOf(player.uniqueId) ?: return@onOpen

        modifyConfig {
            titleBuilder {
                primary("Handel mit ")
                variableValue(partner.name)
            }
        }
    }

    onFirstRender {
        val render = this
        val viewer = player
        val me = viewer.uniqueId

        val session = TradeManager.sessionOf(me)
        val side = session?.sideOf(me)

        if (session == null || side == null) {
            closeForPlayer()
            return@onFirstRender
        }

        val partner = session.partnerOf(me)
        side.context = render

        viewer.scheduler.runAtFixedRate(
            plugin,
            { task -> if (!TradeManager.tick(viewer)) task.cancel() },
            null,
            REFRESH_TICKS,
            REFRESH_TICKS
        )

        for (row in 1..GRID_SIZE) {
            for (column in 1..GRID_SIZE) {
                val index = (row - 1) * GRID_SIZE + (column - 1)

                slot(row, column) {
                    onItemRender { item = session.offerItem(me, index) ?: ownEmptySlotItem }
                    onItemClick {
                        playGeneralClickSound()
                        takeBack(viewer, session, index)
                        update()
                    }
                }

                slot(row, column + PARTNER_COLUMN_OFFSET) {
                    onItemRender { item = session.offerItem(partner.uuid, index) ?: partnerEmptySlotItem }
                }
            }
        }

        slot(GRID_SIZE + 1, 2) {
            onItemRender { item = readyStatusItem(session.isReady(me), "Du") }
        }

        slot(GRID_SIZE + 1, 8) {
            onItemRender { item = readyStatusItem(session.isReady(partner.uuid), partner.name) }
        }

        slot(6, 1) {
            withItem(cancelItem)
            onItemClick {
                playGeneralClickSound()
                TradeManager.cancel(viewer, TradeCancelReason.CLOSED)
                TradeManager.settle(viewer, closeView = true)
            }
        }

        slot(6, 4) {
            withItem(headItem(me, "Du"))
        }

        slot(6, 5) {
            onItemRender { item = readyButtonItem(session, me) }
            onItemClick {
                playGeneralClickSound()
                toggleReady(viewer, session)
                update()
            }
        }

        slot(6, 6) {
            withItem(headItem(partner.uuid, partner.name))
        }

        slot(6, 9) {
            withItem(helpItem)
        }
    }

    onClose {
        TradeManager.cancel(player, TradeCancelReason.CLOSED)
        TradeManager.settle(player, closeView = false)
    }
}

private fun takeBack(viewer: Player, session: TradeSession, index: Int) {
    val item = session.removeItem(viewer.uniqueId, index) ?: return
    val leftover = viewer.inventory.addItem(item).values.firstOrNull() ?: return

    session.restoreItem(viewer.uniqueId, index, leftover)
    viewer.playNoSound()
    viewer.sendText {
        appendErrorPrefix()
        error("Dein Inventar ist voll.")
    }
}

private fun toggleReady(viewer: Player, session: TradeSession) {
    val me = viewer.uniqueId

    if (session.isReady(me)) {
        session.setReady(me, false, System.currentTimeMillis())
        return
    }

    if (!viewer.hasRoomFor(session.offerItems(session.partnerOf(me).uuid))) {
        viewer.playNoSound()
        viewer.sendText {
            appendErrorPrefix()
            error("Dein Inventar hat nicht genug Platz für das Angebot deines Partners.")
        }
        return
    }

    if (!session.setReady(me, true, System.currentTimeMillis())) {
        viewer.playNoSound()
        viewer.sendText {
            appendErrorPrefix()
            error("Es wurde noch nichts angeboten.")
        }
    }
}

private fun readyButtonItem(session: TradeSession, viewer: java.util.UUID): ItemStack {
    if (session.phase == TradePhase.COUNTDOWN) {
        val seconds = ceil((session.countdownEndsAt - System.currentTimeMillis()) / 1000.0).toInt().coerceAtLeast(1)

        return viewIcon(ViewIconType.RELOAD, ViewIconColor.YELLOW) {
            displayName {
                warning("Handel in ")
                variableValue("${seconds}s")
            }

            buildLore {
                emptyLine()
                line { spacer("Klicke, um abzubrechen".toSmallCaps()) }
                emptyLine()
            }
        }
    }

    if (session.isReady(viewer)) {
        return viewIcon(ViewIconType.CHECK, ViewIconColor.YELLOW) {
            displayName { warning("Warte auf deinen Partner") }

            buildLore {
                emptyLine()
                line { spacer("Klicke, um nicht mehr bereit zu sein".toSmallCaps()) }
                emptyLine()
            }
        }
    }

    return readyItem
}

private fun readyStatusItem(ready: Boolean, name: String): ItemStack {
    return viewIcon(
        if (ready) ViewIconType.CHECK else ViewIconType.CROSS,
        if (ready) ViewIconColor.GREEN else ViewIconColor.RED
    ) {
        displayName {
            if (ready) success("$name: Bereit") else error("$name: Nicht bereit")
        }
    }
}

private fun headItem(uuid: java.util.UUID, name: String): ItemStack {
    return buildItem(Material.PLAYER_HEAD) {
        meta<SkullMeta> { owningPlayer = Bukkit.getOfflinePlayer(uuid) }
        displayName { primary(name) }
    }
}


private val ownEmptySlotItem = buildItem(Material.GRAY_STAINED_GLASS_PANE) {
    displayName { spacer("Freier Platz") }

    buildLore {
        emptyLine()
        line { spacer("Klicke ein Item in deinem Inventar an".toSmallCaps()) }
        emptyLine()
    }
}

private val partnerEmptySlotItem = buildItem(Material.GRAY_STAINED_GLASS_PANE) {
    displayName { spacer("Noch nichts angeboten") }
}

private val readyItem = viewIcon(ViewIconType.CHECK, ViewIconColor.GREEN) {
    displayName { success("Bereit") }

    buildLore {
        emptyLine()
        line { spacer("Sind beide bereit, startet der Handel".toSmallCaps()) }
        line { spacer("nach einem kurzen Countdown".toSmallCaps()) }
        emptyLine()
    }
}

private val cancelItem = viewIcon(ViewIconType.CROSS, ViewIconColor.RED) {
    displayName { error("Handel abbrechen") }

    buildLore {
        emptyLine()
        line { spacer("Alle Items gehen zurück an ihre Besitzer".toSmallCaps()) }
        emptyLine()
    }
}

private val helpItem = viewIcon(ViewIconType.QUESTION_MARK, ViewIconColor.WHITE) {
    displayName { primary("So funktioniert der Handel") }

    buildLore {
        emptyLine()
        line { spacer("Linksklick im Inventar: ganzer Stapel".toSmallCaps()) }
        line { spacer("Rechtsklick im Inventar: ein Item".toSmallCaps()) }
        line { spacer("Klick auf dein Angebot: zurücknehmen".toSmallCaps()) }
        line { spacer("Jede Änderung setzt beide auf nicht bereit".toSmallCaps()) }
        line { spacer("Das Fenster zu schließen bricht den Handel ab".toSmallCaps()) }
        emptyLine()
    }
}
