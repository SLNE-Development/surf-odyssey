package dev.slne.surf.odyssey.core.common.player

import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.api.core.util.mutableObjectSetOf
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.core.common.home.HomeBridge
import dev.slne.surf.odyssey.core.common.home.PlayerHomes
import java.util.*

class OdysseyPlayer(
    val uuid: UUID
) : PlayerHomes {
    private val _homes = mutableObjectSetOf<Home>()
    override val homes get() = _homes.freeze()

    override suspend fun createHome(home: Home) {
        if (!_homes.add(home)) {
            return
        }

        HomeBridge.createHome(uuid, home)
    }

    override suspend fun saveHome(home: Home) {
        if (!_homes.contains(home)) {
            return
        }

        HomeBridge.saveHome(uuid, home)
    }

    override suspend fun deleteHome(home: Home) {
        if (!_homes.remove(home)) {
            return
        }

        HomeBridge.deleteHome(uuid, home)
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
            _homes.addAll(dto.homes)
        }
    }
}
