package dev.slne.surf.odyssey.core.common.skill

interface PlayerSkills {
    val skillsLoaded: Boolean

    fun xp(skill: SkillType): Long

    fun level(skill: SkillType): Int = SkillCurve.levelFor(xp(skill))

    fun addXp(skill: SkillType, amount: Long): Boolean

    fun setXp(skill: SkillType, amount: Long)

    fun applySkills(progress: List<SkillProgress>)
}
