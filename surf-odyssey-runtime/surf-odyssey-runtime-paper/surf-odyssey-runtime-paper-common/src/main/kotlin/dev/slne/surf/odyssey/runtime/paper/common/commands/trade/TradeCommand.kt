package dev.slne.surf.odyssey.runtime.paper.common.commands.trade

import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.trade.TRADE_REQUEST_TIMEOUT_MILLIS
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeAcceptResult
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeManager
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeRequestResult
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry
import org.bukkit.Sound
import org.bukkit.entity.Player

fun tradeCommand() = commandTree("trade") {
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

                    TradeRequestResult.ALREADY_PENDING -> player.sendText {
                        appendErrorPrefix()
                        error("Du hast")
                        appendSpace()
                        variableValue(tradePartner.name)
                        appendSpace()
                        error("bereits eine Handelsanfrage gesendet.")
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

    literalArgument("accept") {
        entitySelectorArgumentOnePlayer("tradePartner") {
            playerExecutor { player, args ->
                val sender = args["tradePartner"] as Player

                when (TradeManager.accept(player, sender)) {
                    TradeAcceptResult.STARTED -> Unit

                    TradeAcceptResult.NO_REQUEST -> player.sendText {
                        appendErrorPrefix()
                        error("Du hast keine gültige Handelsanfrage von")
                        appendSpace()
                        variableValue(sender.name)
                        error(".")
                    }

                    TradeAcceptResult.ALREADY_TRADING -> player.sendText {
                        appendErrorPrefix()
                        error("Du befindest dich bereits in einem Handel.")
                    }

                    TradeAcceptResult.PARTNER_BUSY -> player.sendText {
                        appendErrorPrefix()
                        variableValue(sender.name)
                        appendSpace()
                        error("befindet sich bereits in einem Handel.")
                    }
                }
            }
        }
    }

    literalArgument("decline") {
        entitySelectorArgumentOnePlayer("tradePartner") {
            playerExecutor { player, args ->
                val sender = args["tradePartner"] as Player

                if (!TradeManager.decline(player, sender)) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Du hast keine gültige Handelsanfrage von")
                        appendSpace()
                        variableValue(sender.name)
                        error(".")
                    }
                    return@playerExecutor
                }

                player.sendText {
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
                    variableValue(player.name)
                    appendSpace()
                    info("hat deine Handelsanfrage abgelehnt.")
                }
            }
        }
    }
}
