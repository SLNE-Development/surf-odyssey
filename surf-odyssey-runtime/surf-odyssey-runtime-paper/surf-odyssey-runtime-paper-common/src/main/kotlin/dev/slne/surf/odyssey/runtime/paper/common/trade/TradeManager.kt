package dev.slne.surf.odyssey.runtime.paper.common.trade

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.odyssey.core.common.trade.TradeRequestStatus
import dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view.tradeView
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

internal enum class TradeAcceptResult { STARTED, NO_REQUEST, ALREADY_TRADING, PARTNER_BUSY }

private const val EXPIRY_SWEEP_TICKS = 100L

internal object TradeManager {
    private val requests = TradeRequestBook()
    private val sessions = ConcurrentHashMap<UUID, TradeSession>()

    fun sessionOf(uuid: UUID): TradeSession? = sessions[uuid]

    fun isTrading(uuid: UUID) = sessions.containsKey(uuid)

    /** Starts the task that records trade requests nobody answered in time. */
    fun start() {
        plugin.server.globalRegionScheduler.runAtFixedRate(
            plugin,
            { purgeExpired(System.currentTimeMillis()) },
            EXPIRY_SWEEP_TICKS,
            EXPIRY_SWEEP_TICKS
        )
    }

    /**
     * Sends a trade request from [from] to [to].
     *
     * @return [TradeRequestResult.SENT] if the request was created, otherwise the reason it was refused
     */
    fun request(from: Player, to: Player): TradeRequestResult {
        if (isTrading(from.uniqueId)) return TradeRequestResult.ALREADY_TRADING
        if (isTrading(to.uniqueId)) return TradeRequestResult.PARTNER_BUSY
        if (!TradePreferences.isReceiving(to.uniqueId)) return TradeRequestResult.REQUESTS_DISABLED

        val now = System.currentTimeMillis()
        purgeExpired(now)

        return requests.add(from.uniqueId, from.name, to.uniqueId, to.name, now)
    }

    /** The milliseconds [sender] still has to wait before the next trade request can be sent. */
    fun cooldownRemaining(sender: Player) = requests.cooldownRemaining(sender.uniqueId, System.currentTimeMillis())

    /** The valid requests [player] sent or received, received ones first. */
    fun requestsOf(player: Player): List<TradeRequest> = requests.involving(player.uniqueId, System.currentTimeMillis())

    /**
     * Withdraws the pending trade request that [sender] sent to [receiver].
     *
     * @return `true` if a valid request existed and was removed, `false` otherwise
     */
    fun withdrawRequest(sender: Player, receiver: Player): Boolean {
        val request = requests.take(sender.uniqueId, receiver.uniqueId) ?: return false

        return resolve(request, TradeRequestStatus.WITHDRAWN)
    }

    /**
     * Withdraws every pending trade request that [sender] has sent.
     *
     * @return the number of valid requests that were removed
     */
    fun withdrawAllRequests(sender: Player): Int {
        return requests.takeFrom(sender.uniqueId).count { resolve(it, TradeRequestStatus.WITHDRAWN) }
    }

    fun accept(accepter: Player, sender: Player): TradeAcceptResult {
        val session: TradeSession

        synchronized(this) {
            val request = requests.take(sender.uniqueId, accepter.uniqueId) ?: return TradeAcceptResult.NO_REQUEST

            if (request.isExpired(System.currentTimeMillis())) {
                resolve(request, TradeRequestStatus.EXPIRED)
                return TradeAcceptResult.NO_REQUEST
            }

            if (isTrading(accepter.uniqueId)) {
                resolve(request, TradeRequestStatus.CANCELLED)
                return TradeAcceptResult.ALREADY_TRADING
            }

            if (isTrading(sender.uniqueId)) {
                resolve(request, TradeRequestStatus.CANCELLED)
                return TradeAcceptResult.PARTNER_BUSY
            }

            session = TradeSession(
                TradeSide(sender.uniqueId, sender.name),
                TradeSide(accepter.uniqueId, accepter.name)
            )
            sessions[sender.uniqueId] = session
            sessions[accepter.uniqueId] = session

            resolve(request, TradeRequestStatus.ACCEPTED)
            requests.takeInvolving(sender.uniqueId).forEach { resolve(it, TradeRequestStatus.CANCELLED) }
            requests.takeInvolving(accepter.uniqueId).forEach { resolve(it, TradeRequestStatus.CANCELLED) }
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

    /**
     * Declines the pending trade request that [sender] sent to [decliner].
     *
     * @return `true` if a valid request existed and was removed, `false` otherwise
     */
    fun decline(decliner: Player, sender: Player): Boolean {
        val request = requests.take(sender.uniqueId, decliner.uniqueId) ?: return false

        return resolve(request, TradeRequestStatus.DECLINED)
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

        session.takeSnapshot()?.let(TradeLog::trade)

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
        requests.takeInvolving(player.uniqueId).forEach { resolve(it, TradeRequestStatus.CANCELLED) }
        requests.forgetCooldown(player.uniqueId)

        val session = sessions[player.uniqueId] ?: return
        session.cancel(player.uniqueId, TradeCancelReason.LEFT)
        settle(player, closeView = false)
    }

    fun shutdown() {
        requests.takeAll().forEach { resolve(it, TradeRequestStatus.CANCELLED) }

        sessions.values.toSet().forEach { session ->
            session.cancel(null, TradeCancelReason.SHUTDOWN)

            listOf(session.first, session.second).forEach { side ->
                val player = plugin.server.getPlayer(side.uuid) ?: return@forEach
                runCatching { settle(player, closeView = true) }
                    .onFailure { plugin.logger.log(Level.SEVERE, "Failed to return the trade items of ${side.name}", it) }
            }

            session.takeSnapshot()?.let(TradeLog::trade)
        }

        sessions.clear()
    }

    private fun openFor(sender: Player, session: TradeSession) {
        if (sessions[sender.uniqueId] !== session) return

        // Close whatever the sender has open, such as the request list, so the trade window opens on its own
        sender.closeInventory()
        sender.scheduler.run(plugin, { openTradeView(sender, session) }, null)
    }

    private fun openTradeView(sender: Player, session: TradeSession) {
        if (sessions[sender.uniqueId] !== session) return

        try {
            tradeView.open(sender)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to open the trade window of ${sender.name}", e)
            session.cancel(null, TradeCancelReason.CLOSED)
            settle(sender, closeView = false)
        }
    }

    /** Records every request that ran out at [now] as expired. */
    private fun purgeExpired(now: Long) {
        requests.takeExpired(now).forEach { resolve(it, TradeRequestStatus.EXPIRED, now) }
    }

    /**
     * Records how [request] ended; a request that had already run out is recorded as expired whatever [status] says.
     *
     * @return `true` if the request was still valid
     */
    private fun resolve(request: TradeRequest, status: TradeRequestStatus, now: Long = System.currentTimeMillis()): Boolean {
        TradeLog.request(request, status, now)

        return !request.isExpired(now)
    }
}
