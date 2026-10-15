package dev.slne.surf.odyssey.runtime.paper.common.menu.skill

import dev.slne.surf.api.paper.builder.LoreBuilder
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.odyssey.core.common.skill.SkillCurve
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.common.player.cachedPlayer
import dev.slne.surf.odyssey.runtime.paper.common.skill.displayName
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.text.NumberFormat
import java.util.Locale

private const val PROGRESS_BAR_LENGTH = 20

private val SkillType.icon: Material
    get() = when (this) {
        SkillType.MINING -> Material.IRON_PICKAXE
        SkillType.FISHING -> Material.FISHING_ROD
        SkillType.MOB_HUNTING -> Material.IRON_SWORD
    }

private fun formatXp(value: Long): String = NumberFormat.getIntegerInstance(Locale.GERMANY).format(value)

private fun Player.xpOf(skill: SkillType): Long = cachedPlayer()?.xp(skill) ?: 0L

private fun LoreBuilder.progressLines(xp: Long, level: Int) {
    val nextLevel = SkillCurve.xpForLevel(level + 1)
    val fraction = SkillCurve.progressToNextLevel(xp)
    val filled = (fraction * PROGRESS_BAR_LENGTH).toInt().coerceIn(0, PROGRESS_BAR_LENGTH)
    val percent = (fraction * 100).toInt().coerceIn(0, 100)

    line {
        variableKey("XP: ")
        variableValue(formatXp(xp))
        spacer(" / ")
        variableValue(formatXp(nextLevel))
        spacer(" ($percent%)")
    }
    line {
        success("█".repeat(filled))
        spacer("█".repeat(PROGRESS_BAR_LENGTH - filled))
    }
}

internal fun createSkillOverviewItem(player: Player, skill: SkillType): ItemStack {
    val level = SkillCurve.levelFor(player.xpOf(skill))

    return buildItem(skill.icon) {
        addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        displayName {
            primary(skill.displayName)
        }

        buildLore {
            emptyLine()
            line {
                variableKey("Level: ")
                variableValue("$level")
            }
            emptyLine()
        }
    }
}

internal fun createSkillSummaryItem(player: Player, skill: SkillType): ItemStack {
    val xp = player.xpOf(skill)
    val level = SkillCurve.levelFor(xp)

    return buildItem(skill.icon) {
        addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        displayName {
            primary(skill.displayName)
        }

        buildLore {
            emptyLine()
            line {
                variableKey("Level: ")
                variableValue("$level")
            }

            if (level >= SkillCurve.MAX_LEVEL) {
                line {
                    variableKey("XP: ")
                    variableValue(formatXp(xp))
                    spacer(" / ")
                    variableValue(formatXp(SkillCurve.MAX_LEVEL_XP))
                }
                emptyLine()
                line {
                    success("Maximales Level erreicht")
                }
            } else {
                progressLines(xp, level)
            }

            emptyLine()
        }
    }
}

internal fun createLevelItem(player: Player, skill: SkillType, level: Int): ItemStack {
    val xp = player.xpOf(skill)
    val currentLevel = SkillCurve.levelFor(xp)
    val required = SkillCurve.xpForLevel(level)

    val pane = when {
        level <= currentLevel -> Material.LIME_STAINED_GLASS_PANE
        level == currentLevel + 1 -> Material.YELLOW_STAINED_GLASS_PANE
        else -> Material.GRAY_STAINED_GLASS_PANE
    }

    return buildItem(pane, amount = level) {
        if (level == currentLevel) {
            editMeta { it.setEnchantmentGlintOverride(true) }
        }

        displayName {
            when {
                level <= currentLevel -> success("Level $level")
                level == currentLevel + 1 -> primary("Level $level")
                else -> spacer("Level $level")
            }
        }

        buildLore {
            emptyLine()
            when {
                level <= currentLevel -> {
                    line { success("Erreicht") }
                    line {
                        variableKey("Benötigte XP: ")
                        variableValue(formatXp(required))
                    }
                }

                level == currentLevel + 1 -> progressLines(xp, currentLevel)

                else -> line {
                    variableKey("Benötigte XP: ")
                    variableValue(formatXp(required))
                }
            }
            emptyLine()
        }
    }
}
