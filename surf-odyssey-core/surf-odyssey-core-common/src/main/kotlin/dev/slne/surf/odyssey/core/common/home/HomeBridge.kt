package dev.slne.surf.odyssey.core.common.home

import dev.slne.surf.api.core.util.requiredService
import java.util.*

private val bridge = requiredService<HomeBridge>()

interface HomeBridge {
    suspend fun listHomes(playerUuid: UUID): List<Home>
    suspend fun createHome(playerUuid: UUID, home: Home)
    suspend fun saveHome(playerUuid: UUID, home: Home)
    suspend fun deleteHome(playerUuid: UUID, home: Home)

    companion object : HomeBridge by bridge {
        val INSTANCE get() = bridge
    }
}