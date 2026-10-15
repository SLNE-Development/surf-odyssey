package dev.slne.surf.odyssey.runtime.paper.common.commands.skill

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.longArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.odyssey.core.common.skill.SkillBridge
import dev.slne.surf.odyssey.core.common.skill.SkillCurve
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.common.menu.skill.view.skillView
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import dev.slne.surf.odyssey.runtime.paper.common.player.cachedPlayer
import dev.slne.surf.odyssey.runtime.paper.common.skill.displayName
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level

fun skillCommand() = commandTree("skill") {
    withPermission(OdysseyBasePermissionRegistry.SKILL_COMMAND)

    playerExecutor { player, _ ->
        val odysseyPlayer = player.cachedPlayer()

        if (odysseyPlayer == null || !odysseyPlayer.skillsLoaded) {
            player.sendText {
                appendErrorPrefix()
                error("Deine Skills sind gerade nicht verfügbar.")
            }
            return@playerExecutor
        }

        skillView.open(player)
    }

    literalArgument("admin") {
        withPermission(OdysseyBasePermissionRegistry.SKILL_ADMIN)

        literalArgument("set") {
            entitySelectorArgumentOnePlayer("target") {
                stringArgument("skill") {
                    replaceSuggestions(
                        ArgumentSuggestions.strings(*SkillType.entries.map { it.name.lowercase() }.toTypedArray())
                    )

                    literalArgument("xp") {
                        longArgument("amount", min = 0) {
                            anyExecutor { sender, args ->
                                setSkill(sender, args["target"] as Player, args["skill"] as String, args["amount"] as Long)
                            }
                        }
                    }

                    literalArgument("level") {
                        integerArgument("level", min = 0, max = SkillCurve.MAX_LEVEL) {
                            anyExecutor { sender, args ->
                                val level = args["level"] as Int

                                setSkill(sender, args["target"] as Player, args["skill"] as String, SkillCurve.xpForLevel(level))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun setSkill(sender: CommandSender, target: Player, skillName: String, amount: Long) {
    val skill = SkillType.entries.firstOrNull { it.name.equals(skillName, ignoreCase = true) }

    if (skill == null) {
        sender.sendText {
            appendErrorPrefix()
            error("Unbekannter Skill.")
        }
        return
    }

    val xp = SkillCurve.clamp(amount)
    val odysseyPlayer = target.cachedPlayer()?.takeIf { it.skillsLoaded }

    if (odysseyPlayer == null) {
        sender.sendText {
            appendErrorPrefix()
            error("Die Skills von ")
            variableValue(target.name)
            error(" sind nicht geladen.")
        }
        return
    }

    odysseyPlayer.setXp(skill, xp)

    plugin.launch {
        runCatching { SkillBridge.setSkill(target.uniqueId, skill, xp) }
            .onSuccess {
                sender.sendText {
                    appendSuccessPrefix()
                    success("${skill.displayName} von ")
                    variableValue(target.name)
                    success(" auf ")
                    variableValue("$xp XP")
                    success(" (Level ")
                    variableValue("${SkillCurve.levelFor(xp)}")
                    success(") gesetzt.")
                }
            }
            .onFailure {
                plugin.logger.log(Level.SEVERE, "Failed to set $skill experience of ${target.uniqueId}", it)
                sender.sendText {
                    appendErrorPrefix()
                    error("Der Wert wurde lokal gesetzt, aber nicht gespeichert.")
                }
            }
    }
}
