package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.command

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry
import dev.slne.surf.odyssey.runtime.paper.hub.plugin
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorld
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.BlockPosConfig
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.BoxConfig
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.LocationConfig
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.PortalConfig
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.PortalKind
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.SkillWorldSettings
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining.MiningReset
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal.PortalBlocks
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal.PortalRouter
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal.Portals
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.selection.SelectionWand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level

fun skillWorldCommand() = commandTree("skillworld") {
    withPermission(OdysseyBasePermissionRegistry.SKILL_WORLD_COMMAND)

    literalArgument("pos1") {
        playerExecutor { player, _ ->
            Selections.setFirst(player)
            player.sendText {
                appendSuccessPrefix()
                success("Erste Ecke gesetzt.")
            }
        }
    }

    literalArgument("pos2") {
        playerExecutor { player, _ ->
            Selections.setSecond(player)
            player.sendText {
                appendSuccessPrefix()
                success("Zweite Ecke gesetzt.")
            }
        }
    }

    literalArgument("wand") {
        playerExecutor { player, _ ->
            SelectionWand.give(player)
        }
    }

    literalArgument("region") {
        literalArgument("set") {
            stringArgument("skill") {
                replaceSuggestions(skillSuggestions)

                playerExecutor { player, args ->
                    setRegion(player, args["skill"] as String)
                }
            }
        }
    }

    literalArgument("portal") {
        literalArgument("create") {
            stringArgument("id") {
                literalArgument("entry") {
                    stringArgument("skill") {
                        replaceSuggestions(skillSuggestions)

                        playerExecutor { player, args ->
                            val skill = parseSkill(args["skill"] as String) ?: return@playerExecutor player.sendText {
                                appendErrorPrefix()
                                error("Unbekannter Skill.")
                            }
                            createPortal(player, args["id"] as String, PortalKind.ENTRY, skill)
                        }
                    }
                }

                literalArgument("exit") {
                    playerExecutor { player, args ->
                        createPortal(player, args["id"] as String, PortalKind.EXIT, null)
                    }
                }
            }
        }

        literalArgument("delete") {
            stringArgument("id") {
                replaceSuggestions(ArgumentSuggestions.strings { SkillWorldSettings.config.portals.keys.toTypedArray() })

                anyExecutor { sender, args ->
                    deletePortal(sender, args["id"] as String)
                }
            }
        }
    }

    literalArgument("arrival") {
        literalArgument("set") {
            stringArgument("skill") {
                replaceSuggestions(skillSuggestions)

                playerExecutor { player, args ->
                    setArrival(player, args["skill"] as String)
                }
            }
        }
    }

    literalArgument("build") {
        playerExecutor { player, _ ->
            val world = try {
                SkillWorld.openEditWorld()
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "Failed to open the build world", e)
                return@playerExecutor player.sendText {
                    appendErrorPrefix()
                    error("Die Bauwelt konnte nicht geladen werden. Details stehen im Log.")
                }
            }

            player.teleportAsync(world.spawnLocation)
            player.sendText {
                appendSuccessPrefix()
                success("Du bist in der Bauwelt ")
                variableValue(world.name)
                success(".")
            }
        }
    }

    literalArgument("reload") {
        anyExecutor { sender, _ ->
            plugin.launch {
                sender.sendReloadResult(SkillWorld.reload())
            }
        }
    }

    literalArgument("reset") {
        literalArgument("mining") {
            anyExecutor { sender, _ ->
                when (MiningReset.triggerNow()) {
                    MiningReset.TriggerResult.Started -> sender.sendText {
                        appendSuccessPrefix()
                        success("Der Countdown für den Mining-Reset läuft.")
                    }

                    MiningReset.TriggerResult.AlreadyRunning -> sender.sendText {
                        appendErrorPrefix()
                        error("Der Mining-Reset läuft bereits.")
                    }

                    MiningReset.TriggerResult.NoCycle -> sender.sendText {
                        appendErrorPrefix()
                        error("Es läuft kein Mining-Reset. Ist die Skill-Welt geladen und die Mining-Region gesetzt? Nach dem Setzen der Region ist ein /skillworld reload nötig.")
                    }
                }
            }
        }
    }
}

private fun CommandSender.sendReloadResult(result: SkillWorld.ReloadResult) {
    if (result == SkillWorld.ReloadResult.ConfigInvalid) {
        sendText {
            appendErrorPrefix()
            error("Die Datei skill-world.yml konnte nicht gelesen werden. Details stehen im Log.")
        }
    } else if (result == SkillWorld.ReloadResult.UnloadFailed) {
        sendText {
            appendErrorPrefix()
            error("Die alte Skill-Welt konnte nicht entladen werden und bleibt aktiv. Details stehen im Log.")
        }
    } else if (result == SkillWorld.ReloadResult.BuildMode) {
        sendText {
            appendSuccessPrefix()
            success("Kein gültiger Weltname gesetzt, die Bauwelt ist aktiv. Die Portale bleiben deaktiviert.")
        }
    } else if (result == SkillWorld.ReloadResult.Reloaded) {
        sendText {
            appendSuccessPrefix()
            success("Die Skill-Welt wurde neu geladen.")
        }
    } else {
        sendText {
            appendErrorPrefix()
            error("Die Skill-Welt konnte nicht geladen werden. Details stehen im Log.")
        }
    }
}

private fun setRegion(player: Player, skillName: String) {
    val skill = parseSkill(skillName) ?: return player.sendText {
        appendErrorPrefix()
        error("Unbekannter Skill.")
    }
    if (!SkillWorld.isEditWorld(player.world)) return player.sendText {
        appendErrorPrefix()
        error("Regionen können nur in der Skill-Welt oder der Bauwelt gesetzt werden.")
    }
    val (a, b) = Selections.corners(player) ?: return player.sendText {
        appendErrorPrefix()
        error("Setze zuerst beide Ecken mit /skillworld pos1 und pos2 in dieser Welt.")
    }

    SkillWorldSettings.edit { it.copy(regions = it.regions + (skill to BoxConfig(BlockPosConfig.of(a), BlockPosConfig.of(b)))) }
    SkillWorld.refreshRegions()

    player.sendText {
        appendSuccessPrefix()
        success("Region für ")
        variableValue(skill.name.lowercase())
        success(" gesetzt.")
        if (skill == SkillType.MINING || skill == SkillType.MOB_HUNTING) {
            info(" Für Mining-Reset und Mob-Spawner ist ein /skillworld reload nötig.")
        }
    }
}

private fun createPortal(player: Player, id: String, kind: PortalKind, skill: SkillType?) {
    val expectedHub = PortalRouter.expectedWorldIsHub(kind)
    val inExpectedWorld = if (expectedHub) player.world == SkillWorld.hubWorld() else SkillWorld.isEditWorld(player.world)
    if (!inExpectedWorld) return player.sendText {
        appendErrorPrefix()
        error(if (expectedHub) "Eingangsportale gehören in die Hub-Welt." else "Rückportale gehören in die Skill-Welt.")
    }
    val (a, b) = Selections.corners(player) ?: return player.sendText {
        appendErrorPrefix()
        error("Setze zuerst beide Ecken mit /skillworld pos1 und pos2 in dieser Welt.")
    }
    SkillWorldSettings.edit {
        it.copy(portals = it.portals + (id to PortalConfig(kind, skill, BlockPosConfig.of(a), BlockPosConfig.of(b))))
    }
    Portals.refresh()

    player.sendText {
        appendSuccessPrefix()
        success("Portal ")
        variableValue(id)
        success(" gespeichert.")
    }
}

private fun deletePortal(sender: CommandSender, id: String) {
    if (id !in SkillWorldSettings.config.portals) return sender.sendText {
        appendErrorPrefix()
        error("Unbekanntes Portal.")
    }

    SkillWorldSettings.edit { it.copy(portals = it.portals - id) }
    Portals.refresh()

    sender.sendText {
        appendSuccessPrefix()
        success("Portal ")
        variableValue(id)
        success(" gelöscht.")
    }
}

private fun setArrival(player: Player, skillName: String) {
    val skill = parseSkill(skillName) ?: return player.sendText {
        appendErrorPrefix()
        error("Unbekannter Skill.")
    }
    if (!SkillWorld.isEditWorld(player.world)) return player.sendText {
        appendErrorPrefix()
        error("Ankunftspunkte können nur in der Skill-Welt oder der Bauwelt gesetzt werden.")
    }

    val location = player.location
    SkillWorldSettings.edit {
        it.copy(arrivals = it.arrivals + (skill to LocationConfig(location.x, location.y, location.z, location.yaw, location.pitch)))
    }

    player.sendText {
        appendSuccessPrefix()
        success("Ankunftspunkt für ")
        variableValue(skill.name.lowercase())
        success(" gesetzt.")
    }
}
