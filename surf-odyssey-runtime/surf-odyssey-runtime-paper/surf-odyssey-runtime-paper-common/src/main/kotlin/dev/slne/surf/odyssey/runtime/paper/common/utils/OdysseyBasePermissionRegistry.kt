package dev.slne.surf.odyssey.runtime.paper.common.utils

import dev.slne.surf.api.paper.permission.PermissionRegistry
import dev.slne.surf.odyssey.core.common.skill.SkillType

object OdysseyBasePermissionRegistry : PermissionRegistry() {
    private const val PREFIX = "surf.odyssey"
    private const val COMMAND_PREFIX = "$PREFIX.command"

    private const val HOME_BASE = "$PREFIX.home"
    private const val HOME_COMMAND_BASE = "$COMMAND_PREFIX.home"

    private const val SKILL_BASE = "$PREFIX.skill"
    private const val SKILL_COLLECT_BASE = "$SKILL_BASE.collect"
    private const val SKILL_COMMAND_BASE = "$COMMAND_PREFIX.skill"

    // Home
    val HOME_COMMAND = create("$HOME_COMMAND_BASE.use")
    val HOME_CREATE = create("$HOME_BASE.create")
    val HOME_DELETE = create("$HOME_BASE.delete")
    val HOME_LIST = create("$HOME_BASE.list")
    val HOME_EDIT = create("$HOME_BASE.edit")

    //Skill
    val SKILL_COMMAND = create("$SKILL_COMMAND_BASE.use")
    val SKILL_ADMIN = create("$SKILL_BASE.admin")
    val SKILL_COLLECT_MINING = create("$SKILL_COLLECT_BASE.mining")
    val SKILL_COLLECT_FISHING = create("$SKILL_COLLECT_BASE.fishing")
    val SKILL_COLLECT_MOB_HUNTING = create("$SKILL_COLLECT_BASE.mobhunting")
    val SKILL_WORLD_COMMAND = create("$COMMAND_PREFIX.skillworld")

    // Hub
    val HUB_DOUBLE_JUMP = create("$PREFIX.hub.doublejump")

    fun skillCollect(skill: SkillType) = when (skill) {
        SkillType.MINING -> SKILL_COLLECT_MINING
        SkillType.FISHING -> SKILL_COLLECT_FISHING
        SkillType.MOB_HUNTING -> SKILL_COLLECT_MOB_HUNTING
    }
}