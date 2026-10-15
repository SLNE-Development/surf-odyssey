package dev.slne.surf.odyssey.core.common.player

import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.api.core.util.toObjectSet
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.core.common.home.HomeBridge
import dev.slne.surf.odyssey.core.common.home.PlayerHomes
import dev.slne.surf.odyssey.core.common.skill.PlayerSkills
import dev.slne.surf.odyssey.core.common.skill.SkillCurve
import dev.slne.surf.odyssey.core.common.skill.SkillProgress
import dev.slne.surf.odyssey.core.common.skill.SkillType
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A player and their homes as cached on this server.
 *
 * Every change is sent to the microservice first and only applied to the cache once it succeeded, so the cache never
 * holds a home the database does not.
 */
class OdysseyPlayer(
    val uuid: UUID
) : PlayerHomes, PlayerSkills {
    private val _homes = ConcurrentHashMap<UUID, Home>()
    override val homes get() = _homes.values.toObjectSet().freeze()

    private val skillXp = ConcurrentHashMap<SkillType, Long>()

    @Volatile
    private var loadedSkills = false
    override val skillsLoaded get() = loadedSkills

    override fun xp(skill: SkillType): Long = skillXp[skill] ?: 0L

    override fun addXp(skill: SkillType, amount: Long): Boolean {
        if (!loadedSkills || amount <= 0) {
            return false
        }

        // Experience keeps growing past the max level, only the Long range limits it
        skillXp.compute(skill) { _, old ->
            val current = old ?: 0L
            if (current > Long.MAX_VALUE - amount) Long.MAX_VALUE else current + amount
        }
        return true
    }

    override fun setXp(skill: SkillType, amount: Long) {
        skillXp[skill] = SkillCurve.clamp(amount)
    }

    override fun applySkills(progress: List<SkillProgress>) {
        progress.forEach { skillXp[it.skill] = SkillCurve.clamp(it.xp) }
        loadedSkills = true
    }

    /** The cached home with [id], or null if this player has none. */
    fun home(id: UUID): Home? = _homes[id]

    /** Whether another home than [except] is already called [name], ignoring case. */
    fun hasHomeNamed(name: String, except: Home? = null): Boolean =
        _homes.values.any { it.id != except?.id && it.name.equals(name, ignoreCase = true) }

    override suspend fun createHome(home: Home) {
        HomeBridge.createHome(uuid, home)
        _homes[home.id] = home
    }

    /** Stores [home], inserting it when it is new and replacing the cached home with the same id otherwise. */
    override suspend fun saveHome(home: Home) {
        HomeBridge.saveHome(uuid, home)
        _homes[home.id] = home
    }

    override suspend fun deleteHome(home: Home) {
        HomeBridge.deleteHome(uuid, home)
        _homes.remove(home.id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OdysseyPlayer

        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    companion object {
        fun fromDto(dto: OdysseyPlayerDto) = OdysseyPlayer(dto.uuid).apply {
            dto.homes.forEach { _homes[it.id] = it }
        }
    }
}
