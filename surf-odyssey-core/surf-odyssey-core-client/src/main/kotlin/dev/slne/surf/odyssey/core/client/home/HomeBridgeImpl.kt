package dev.slne.surf.odyssey.core.client.home

import com.google.auto.service.AutoService
import dev.slne.surf.odyssey.core.client.OdysseyClientInstance
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.core.common.home.HomeBridge
import dev.slne.surf.odyssey.core.common.home.HomeService
import java.util.*

private val proxy by lazy {
    OdysseyClientInstance.INSTANCE.rabbitApi.createRpcService<HomeService>()
}

@AutoService(HomeBridge::class)
class HomeBridgeImpl : HomeBridge {
    override suspend fun listHomes(playerUuid: UUID): List<Home> {
        return proxy.listHomes(playerUuid)
    }

    override suspend fun createHome(
        playerUuid: UUID,
        home: Home
    ) {
        proxy.createHome(playerUuid, home)
    }

    override suspend fun saveHome(
        playerUuid: UUID,
        home: Home
    ) {
        proxy.saveHome(playerUuid, home)
    }

    override suspend fun deleteHome(
        playerUuid: UUID,
        home: Home
    ) {
        proxy.deleteHome(playerUuid, home)
    }
}