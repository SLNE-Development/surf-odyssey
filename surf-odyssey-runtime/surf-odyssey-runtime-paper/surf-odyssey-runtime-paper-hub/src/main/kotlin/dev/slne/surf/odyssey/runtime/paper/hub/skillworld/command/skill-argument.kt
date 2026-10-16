package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.command

import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.slne.surf.odyssey.core.common.skill.SkillType
import org.bukkit.command.CommandSender

val skillSuggestions: ArgumentSuggestions<CommandSender> =
    ArgumentSuggestions.strings(*SkillType.entries.map { it.name.lowercase() }.toTypedArray())

fun parseSkill(name: String): SkillType? = SkillType.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
