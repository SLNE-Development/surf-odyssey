package dev.slne.surf.odyssey.runtime.paper.common.skill

import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.odyssey.core.common.skill.SkillCurve
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.common.player.cachedPlayer
import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

val SkillType.displayName: String
    get() = when (this) {
        SkillType.MINING -> "Mining"
        SkillType.FISHING -> "Fishing"
        SkillType.MOB_HUNTING -> "Mobhunting"
    }

fun Player.awardSkillXp(skill: SkillType, amount: Long): Boolean {
    if (gameMode != GameMode.SURVIVAL) return false
    if (!hasPermission(OdysseyBasePermissionRegistry.skillCollect(skill))) return false

    return grantSkillXp(skill, amount)
}

private const val LEVEL_UP_DISPLAY_MILLIS = 2500L

private val levelUpDisplayUntil = ConcurrentHashMap<UUID, Long>()

internal fun forgetLevelUpDisplay(uuid: UUID) {
    levelUpDisplayUntil.remove(uuid)
}

fun Player.grantSkillXp(skill: SkillType, amount: Long): Boolean {
    val odysseyPlayer = cachedPlayer() ?: return false
    val levelBefore = odysseyPlayer.level(skill)
    if (!odysseyPlayer.addXp(skill, amount)) return false

    SkillRuntime.scheduler.markDirty(uniqueId, skill)

    val levelAfter = odysseyPlayer.level(skill)
    val now = System.currentTimeMillis()

    when {
        levelAfter > levelBefore -> {
            levelUpDisplayUntil[uniqueId] = now + LEVEL_UP_DISPLAY_MILLIS
            sendLevelUpActionBar(skill, levelAfter)
            playSound(true) {
                type(Sound.ENTITY_PLAYER_LEVELUP)
                volume(.6f)
            }
        }

        // Keeps the level up message readable while the next gains come in
        now < (levelUpDisplayUntil[uniqueId] ?: 0L) -> Unit

        else -> {
            sendXpActionBar(skill, amount)
            playSound(true) {
                type(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
                volume(.3f)
            }
        }
    }

    return true
}

private fun Player.sendLevelUpActionBar(skill: SkillType, level: Int) {
    sendActionBar(
        buildText {
            primary(skill.displayName)
            spacer(" >> ")
            success("Level Up!")
            spacer(" Level ")
            variableValue("$level")
        }
    )
}

private fun Player.sendXpActionBar(skill: SkillType, amount: Long) {
    val odysseyPlayer = cachedPlayer() ?: return
    val level = odysseyPlayer.level(skill)
    val xp = odysseyPlayer.xp(skill)
    val needed = if (level >= SkillCurve.MAX_LEVEL) SkillCurve.MAX_LEVEL_XP else SkillCurve.xpForLevel(level + 1)
    val progress = "$xp/$needed"

    sendActionBar(
        buildText {
            primary(skill.displayName)
            spacer(" >> ")
            success("+${amount}XP")
            spacer(" ($progress)")
        }
    )
}