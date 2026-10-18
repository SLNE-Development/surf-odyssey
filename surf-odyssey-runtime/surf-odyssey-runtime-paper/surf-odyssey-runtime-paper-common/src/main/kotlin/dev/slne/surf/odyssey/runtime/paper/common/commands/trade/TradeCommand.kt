package dev.slne.surf.odyssey.runtime.paper.common.commands.trade

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.odyssey.core.common.trade.TradeBridge
import dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view.openTradeHistory
import dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view.tradeListView
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import dev.slne.surf.odyssey.runtime.paper.common.trade.MAX_OUTGOING_TRADE_REQUESTS
import dev.slne.surf.odyssey.runtime.paper.common.trade.TRADE_REQUEST_TIMEOUT_MILLIS
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeManager
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradePreferences
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeRequestResult
import dev.slne.surf.odyssey.runtime.paper.common.trade.acceptTradeRequest
import dev.slne.surf.odyssey.runtime.paper.common.trade.declineTradeRequest
import dev.slne.surf.odyssey.runtime.paper.common.trade.withdrawTradeRequest
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry
import kotlinx.coroutines.CancellationException
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.logging.Level
import kotlin.math.ceil

private const val TRADE_HISTORY_LIMIT = 200

fun tradeCommand() =commandTree("trade") {
    withPermission(OdysseyBasePermissionRegistry.TRADE_COMMAND)

    literalArgument("request") {
        entitySelectorArgumentOnePlayer("tradePartner") {
            playerExecutor { player, args ->
                val tradePartner = args["tradePartner"] as Player

                if (tradePartner == player) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Du kannst dir selbst keine Handelsanfrage senden.")
                    }
                    return@playerExecutor
                }

                when (TradeManager.request(player, tradePartner)) {
                    TradeRequestResult.ALREADY_TRADING -> player.sendText {
                        appendErrorPrefix()
                        error("Du befindest dich bereits in einem Handel.")
                    }

                    TradeRequestResult.PARTNER_BUSY -> player.sendText {
                        appendErrorPrefix()
                        variableValue(tradePartner.name)
                        appendSpace()
                        error("befindet sich bereits in einem Handel.")
                    }

                    TradeRequestResult.REQUESTS_DISABLED -> player.sendText {
                        appendErrorPrefix()
                        variableValue(tradePartner.name)
                        appendSpace()
                        error("nimmt keine Handelsanfragen an.")
                    }

                    TradeRequestResult.COOLDOWN -> player.sendText {
                        val seconds = ceil(TradeManager.cooldownRemaining(player) / 1000.0).toInt().coerceAtLeast(1)

                        appendErrorPrefix()
                        error("Bitte warte noch")
                        appendSpace()
                        variableValue("$seconds")
                        appendSpace()
                        error(if (seconds == 1) "Sekunde, bevor du die nächste Handelsanfrage sendest." else "Sekunden, bevor du die nächste Handelsanfrage sendest.")
                    }

                    TradeRequestResult.ALREADY_PENDING -> player.sendText {
                        appendErrorPrefix()
                        error("Du hast")
                        appendSpace()
                        variableValue(tradePartner.name)
                        appendSpace()
                        error("bereits eine Handelsanfrage gesendet.")
                    }

                    TradeRequestResult.LIMIT_REACHED -> player.sendText {
                        appendErrorPrefix()
                        error("Du hast bereits")
                        appendSpace()
                        variableValue("$MAX_OUTGOING_TRADE_REQUESTS")
                        appendSpace()
                        error("offene Handelsanfragen. Ziehe eine mit /trade withdraw zurück.")
                    }

                    TradeRequestResult.SENT -> {
                        player.sendText {
                            appendInfoPrefix()
                            info("Du hast eine Handelsanfrage an")
                            appendSpace()
                            variableValue(tradePartner.name)
                            appendSpace()
                            info("gesendet. Sie ist ${TRADE_REQUEST_TIMEOUT_MILLIS / 1000} Sekunden gültig.")
                        }

                        tradePartner.playSound(true) {
                            type(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
                        }

                        tradePartner.sendText {
                            appendInfoPrefix()
                            info("Du hast eine Handelsanfrage von")
                            appendSpace()
                            variableValue(player.name)
                            appendSpace()
                            info("erhalten.")
                            appendSpace()
                            append(buildText {
                                success("[Akzeptieren]")
                                clickRunsCommand("/trade accept ${player.name}")
                                hoverEvent(buildText {
                                    info("Klicke, um den Handel mit ")
                                    variableValue(player.name)
                                    info(" zu starten und das Handelsfenster zu öffnen")
                                })
                            })
                            appendSpace()
                            append(buildText {
                                error("[Ablehnen]")
                                clickRunsCommand("/trade decline ${player.name}")
                                hoverEvent(buildText {
                                    info("Klicke, um die Handelsanfrage von ")
                                    variableValue(player.name)
                                    info(" abzulehnen")
                                })
                            })
                        }
                    }
                }
            }
        }
    }

    literalArgument("withdraw") {
        playerExecutor { player, _ ->
            val withdrawn = TradeManager.withdrawAllRequests(player)

            if (withdrawn == 0) {
                player.sendText {
                    appendErrorPrefix()
                    error("Du hast keine gültige Handelsanfrage.")
                }
                return@playerExecutor
            }

            player.sendText {
                appendInfoPrefix()
                info(
                    if (withdrawn == 1) "Du hast deine Handelsanfrage zurückgezogen."
                    else "Du hast deine $withdrawn Handelsanfragen zurückgezogen."
                )
            }
        }

        entitySelectorArgumentOnePlayer("tradePartner") {
            playerExecutor { player, args ->
                player.withdrawTradeRequest(args["tradePartner"] as Player)
            }
        }
    }

    literalArgument("accept") {
        entitySelectorArgumentOnePlayer("tradePartner") {
            playerExecutor { player, args ->
                player.acceptTradeRequest(args["tradePartner"] as Player)
            }
        }
    }

    literalArgument("decline") {
        entitySelectorArgumentOnePlayer("tradePartner") {
            playerExecutor { player, args ->
                player.declineTradeRequest(args["tradePartner"] as Player)
            }
        }
    }

    literalArgument("list") {
        playerExecutor { player, _ ->
            if (TradeManager.requestsOf(player).isEmpty()) {
                player.sendText {
                    appendInfoPrefix()
                    info("Du hast keine offenen Handelsanfragen.")
                }
                return@playerExecutor
            }

            tradeListView.open(player)
        }
    }

    literalArgument("history") {
        playerExecutor { player, _ ->
            plugin.launch {
                try {
                    val trades = TradeBridge.listTrades(player.uniqueId, TRADE_HISTORY_LIMIT)

                    if (trades.isEmpty()) {
                        player.sendText {
                            appendInfoPrefix()
                            info("Du hast noch keine Handel durchgeführt.")
                        }
                        return@launch
                    }

                    player.scheduler.run(plugin, { openTradeHistory(player, trades) }, null)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    plugin.logger.log(Level.SEVERE, "Failed to load the trade history of ${player.name}", e)

                    player.playNoSound()
                    player.sendText {
                        appendErrorPrefix()
                        error("Dein Handelsverlauf konnte nicht geladen werden.")
                    }
                }
            }
        }
    }

    literalArgument("toggle") {
        playerExecutor { player, _ ->
            plugin.launch {
                try {
                    val enabled = !TradePreferences.isReceiving(player.uniqueId)
                    TradePreferences.setReceiving(player.uniqueId, enabled)

                    player.sendText {
                        appendInfoPrefix()
                        if (enabled) {
                            info("Du empfängst jetzt wieder Handelsanfragen.")
                        } else {
                            info("Du empfängst keine Handelsanfragen mehr.")
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    plugin.logger.log(Level.SEVERE, "Failed to toggle the trade requests of ${player.name}", e)

                    player.playNoSound()
                    player.sendText {
                        appendErrorPrefix()
                        error("Die Einstellung konnte nicht gespeichert werden.")
                    }
                }
            }
        }
    }
}
