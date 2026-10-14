package dev.slne.surf.odyssey.core.common.skill

/** The skill experience of one player, held in memory while the player is online. */
interface PlayerSkills {
    /** False until the skills were loaded from the microservice. No experience is granted before. */
    val skillsLoaded: Boolean

    /** The total experience of [skill], zero if none was gained. */
    fun xp(skill: SkillType): Long

    /** The level of [skill], derived from [xp]. */
    fun level(skill: SkillType): Int = SkillCurve.levelFor(xp(skill))

    /**
     * Adds [amount] experience to [skill], capped at [SkillCurve.MAX_XP].
     * Returns false and changes nothing if the skills are not loaded or [amount] is not positive.
     */
    fun addXp(skill: SkillType, amount: Long): Boolean

    /** Replaces the experience of [skill] with [amount], clamped to the valid range. */
    fun setXp(skill: SkillType, amount: Long)

    /** Replaces the experience with the stored [progress] and marks the skills as loaded. */
    fun applySkills(progress: List<SkillProgress>)
}
