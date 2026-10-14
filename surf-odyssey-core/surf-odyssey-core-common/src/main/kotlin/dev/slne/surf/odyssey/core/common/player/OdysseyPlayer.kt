package dev.slne.surf.odyssey.core.common.player

import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.api.core.util.toObjectSet
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.core.common.home.HomeBridge
import dev.slne.surf.odyssey.core.common.home.PlayerHomes
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
) : PlayerHomes {
    private val _homes = ConcurrentHashMap<UUID, Home>()
    override val homes get() = _homes.values.toObjectSet().freeze()

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
