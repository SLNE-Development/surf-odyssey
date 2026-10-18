package dev.slne.surf.odyssey.runtime.paper.common.trade

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import org.bukkit.entity.Player

/**
 * Accepts the trade request [sender] sent to this player and tells this player if that is not possible.
 *
 * @return the result of the accept, [TradeAcceptResult.STARTED] when the trade window opens
 */
internal fun Player.acceptTradeRequest(sender: Player): TradeAcceptResult {
    val result = TradeManager.accept(this, sender)

    when (result) {
        TradeAcceptResult.STARTED -> Unit

        TradeAcceptResult.NO_REQUEST -> sendText {
            appendErrorPrefix()
            error("Du hast keine gültige Handelsanfrage von")
            appendSpace()
            variableValue(sender.name)
            error(".")
        }

        TradeAcceptResult.ALREADY_TRADING -> sendText {
            appendErrorPrefix()
            error("Du befindest dich bereits in einem Handel.")
        }

        TradeAcceptResult.PARTNER_BUSY -> sendText {
            appendErrorPrefix()
            variableValue(sender.name)
            appendSpace()
            error("befindet sich bereits in einem Handel.")
        }
    }

    return result
}

/**
 * Declines the trade request [sender] sent to this player, tells both players and tells this player if there was no
 * valid request.
 *
 * @return `true` if a valid request was declined
 */
internal fun Player.declineTradeRequest(sender: Player): Boolean {
    if (!TradeManager.decline(this, sender)) {
        sendText {
            appendErrorPrefix()
            error("Du hast keine gültige Handelsanfrage von")
            appendSpace()
            variableValue(sender.name)
            error(".")
        }
        return false
    }

    sendText {
        appendInfoPrefix()
        info("Du hast die Handelsanfrage von")
        appendSpace()
        variableValue(sender.name)
        appendSpace()
        info("abgelehnt.")
    }

    sender.playNoSound()
    sender.sendText {
        appendInfoPrefix()
        variableValue(this@declineTradeRequest.name)
        appendSpace()
        info("hat deine Handelsanfrage abgelehnt.")
    }

    return true
}

/**
 * Withdraws the trade request this player sent to [receiver] and tells this player the result.
 *
 * @return `true` if a valid request was withdrawn
 */
internal fun Player.withdrawTradeRequest(receiver: Player): Boolean {
    if (!TradeManager.withdrawRequest(this, receiver)) {
        sendText {
            appendErrorPrefix()
            error("Du hast keine gültige Handelsanfrage an")
            appendSpace()
            variableValue(receiver.name)
            error(".")
        }
        return false
    }

    sendText {
        appendInfoPrefix()
        info("Du hast deine Handelsanfrage an")
        appendSpace()
        variableValue(receiver.name)
        appendSpace()
        info("zurückgezogen.")
    }

    return true
}
