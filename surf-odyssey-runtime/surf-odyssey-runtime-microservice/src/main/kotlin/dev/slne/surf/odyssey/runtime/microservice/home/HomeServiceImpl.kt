package dev.slne.surf.odyssey.runtime.microservice.home

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.core.common.home.HomeService

class HomeServerImpl : HomeService {
    override suspend fun listHomes(playerUuid: SerializableUUID): List<Home> {
        TODO("Not yet implemented")
    }

    override suspend fun createHome(
        playerUuid: SerializableUUID,
        home: Home
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun saveHome(
        playerUuid: SerializableUUID,
        home: Home
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteHome(
        playerUuid: SerializableUUID,
        home: Home
    ) {
        TODO("Not yet implemented")
    }
}