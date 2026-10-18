package dev.slne.surf.odyssey.runtime.paper.common.trade

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view.tradeView
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

internal const val TRADE_REQUEST_TIMEOUT_MILLIS = 60_000L

internal enum class TradeRequestResult { SENT, ALREADY_TRADING, PARTNER_BUSY, ALREADY_PENDING }

internal enum class TradeAcceptResult { STARTED, NO_REQUEST, ALREADY_TRADING, PARTNER_BUSY }

internal object TradeManager {
    private data class RequestKey(val from: UUID, val to: UUID)

    private val requests = ConcurrentHashMap<RequestKey, Long>()
    private val sessions = ConcurrentHashMap<UUID, TradeSession>()

    fun sessionOf(uuid: UUID): TradeSession? = sessions[uuid]

    fun isTrading(uuid: UUID) = sessions.containsKey(uuid)

    fun request(from: Player, to: Player): TradeRequestResult {
        if (isTrading(from.uniqueId)) return TradeRequestResult.ALREADY_TRADING
        if (isTrading(to.uniqueId)) return TradeRequestResult.PARTNER_BUSY

        val now = System.currentTimeMillis()
        requests.values.removeIf { it <= now }

        val key = RequestKey(from.uniqueId, to.uniqueId)
        if (requests.putIfAbsent(key, now + TRADE_REQUEST_TIMEOUT_MILLIS) != null) {
            return TradeRequestResult.ALREADY_PENDING
        }

        return TradeRequestResult.SENT
    }

    fun withdrawRequest(sender: Player, receiver: Player): Boolean {
        val expiresAt = requests.remove(RequestKey(sender.uniqueId, receiver.uniqueId))
        return expiresAt != null && expiresAt > System.currentTimeMillis()
    }

    fun withdrawAllRequests(sender: Player): Int {
        val now = System.currentTimeMillis()
        var withdrawn = 0

        requests.entries.removeIf { (key, expiresAt) ->
            val ownedBySender = key.from == sender.uniqueId
            if (ownedBySender && expiresAt > now) withdrawn++
            ownedBySender
        }

        return withdrawn
    }

    fun accept(accepter: Player, sender: Player): TradeAcceptResult {
        val session: TradeSession

        synchronized(this) {
            val expiresAt = requests.remove(RequestKey(sender.uniqueId, accepter.uniqueId))
            if (expiresAt == null || expiresAt <= System.currentTimeMillis()) return TradeAcceptResult.NO_REQUEST
            if (isTrading(accepter.uniqueId)) return TradeAcceptResult.ALREADY_TRADING
            if (isTrading(sender.uniqueId)) return TradeAcceptResult.PARTNER_BUSY

            session = TradeSession(
                TradeSide(sender.uniqueId, sender.name),
                TradeSide(accepter.uniqueId, accepter.name)
            )
            sessions[sender.uniqueId] = session
            sessions[accepter.uniqueId] = session

            requests.keys.removeIf {
                it.from == sender.uniqueId || it.to == sender.uniqueId ||
                    it.from == accepter.uniqueId || it.to == accepter.uniqueId
            }
        }

        try {
            tradeView.open(accepter)
            sender.scheduler.run(plugin, { openFor(sender, session) }, null)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to open the trade window of ${accepter.name}", e)
            session.cancel(null, TradeCancelReason.CLOSED)
            settle(accepter, closeView = false)
            sender.scheduler.run(plugin, { settle(sender, closeView = false) }, null)
        }

        return TradeAcceptResult.STARTED
    }

    fun decline(decliner: Player, sender: Player): Boolean {
        return requests.remove(RequestKey(sender.uniqueId, decliner.uniqueId)) != null
    }

    fun cancel(player: Player, reason: TradeCancelReason) {
        sessions[player.uniqueId]?.cancel(player.uniqueId, reason)
    }

    fun tick(player: Player): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        session.tryComplete(System.currentTimeMillis())

        if (session.phase.isTerminal) {
            settle(player, closeView = true)
            return false
        }

        session.sideOf(player.uniqueId)?.context?.update()
        return true
    }

    fun settle(player: Player, closeView: Boolean) {
        val session = sessions[player.uniqueId] ?: return
        val settlement = session.settle(player.uniqueId) ?: return
        sessions.remove(player.uniqueId, session)

        val side = session.sideOf(player.uniqueId)
        val context = side?.context
        side?.context = null

        player.giveOrDrop(settlement.items)

        if (closeView) context?.closeForPlayer()

        val partnerName = session.partnerOf(player.uniqueId).name

        if (settlement.completed) {
            player.playYesSound()
            player.sendText {
                appendSuccessPrefix()
                success("Der Handel mit ")
                variableValue(partnerName)
                success(" wurde abgeschlossen.")
            }
            return
        }

        player.playNoSound()
        player.sendText {
            appendErrorPrefix()
            when {
                settlement.cancelReason == TradeCancelReason.SHUTDOWN -> error("Der Handel wurde wegen eines Serverneustarts abgebrochen.")
                settlement.cancelledBy == player.uniqueId -> error("Du hast den Handel abgebrochen.")
                settlement.cancelReason == TradeCancelReason.LEFT -> {
                    variableValue(partnerName)
                    error(" hat den Server verlassen. Der Handel wurde abgebrochen.")
                }

                else -> {
                    variableValue(partnerName)
                    error(" hat den Handel abgebrochen.")
                }
            }
        }

        if (settlement.items.isNotEmpty()) {
            player.sendText {
                appendInfoPrefix()
                info("Deine Items wurden dir zurückgegeben.")
            }
        }
    }

    fun forget(player: Player) {
        requests.keys.removeIf { it.from == player.uniqueId || it.to == player.uniqueId }

        val session = sessions[player.uniqueId] ?: return
        session.cancel(player.uniqueId, TradeCancelReason.LEFT)
        settle(player, closeView = false)
    }

    fun shutdown() {
        requests.clear()

        sessions.values.toSet().forEach { session ->
            session.cancel(null, TradeCancelReason.SHUTDOWN)

            listOf(session.first, session.second).forEach { side ->
                val player = plugin.server.getPlayer(side.uuid) ?: return@forEach
                runCatching { settle(player, closeView = true) }
                    .onFailure { plugin.logger.log(Level.SEVERE, "Failed to return the trade items of ${side.name}", it) }
            }
        }

        sessions.clear()
    }

    private fun openFor(sender: Player, session: TradeSession) {
        if (sessions[sender.uniqueId] !== session) return

        try {
            tradeView.open(sender)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to open the trade window of ${sender.name}", e)
            session.cancel(null, TradeCancelReason.CLOSED)
            settle(sender, closeView = false)
        }
    }
}
