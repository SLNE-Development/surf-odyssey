package dev.slne.surf.odyssey.runtime.paper.hub.skillworld

import dev.slne.surf.odyssey.core.common.skill.SkillType
import java.util.concurrent.ConcurrentHashMap

object SkillLocks {
    private val locked: MutableSet<SkillType> = ConcurrentHashMap.newKeySet()

    fun lock(skill: SkillType) {
        locked += skill
    }

    fun unlock(skill: SkillType) {
        locked -= skill
    }

    fun isLocked(skill: SkillType): Boolean = skill in locked

    fun snapshot(): Set<SkillType> = locked.toSet()
}
