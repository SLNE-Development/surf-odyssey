package dev.slne.surf.odyssey.runtime.microservice.player

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertIgnore
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.odyssey.core.common.player.OdysseyPlayerDto
import dev.slne.surf.odyssey.core.common.player.PlayerService
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable
import dev.slne.surf.odyssey.runtime.microservice.home.selectHomes
import kotlinx.coroutines.flow.firstOrNull

object PlayerServiceImpl : PlayerService {
    override suspend fun findOrCreatePlayer(uuid: SerializableUUID): OdysseyPlayerDto = suspendTransaction {
        // Does nothing when the player already exists, so concurrent first joins cannot fail
        OdysseyPlayerTable.insertIgnore { it[OdysseyPlayerTable.uuid] = uuid }

        OdysseyPlayerDto(uuid, selectHomes(uuid))
    }

    override suspend fun findPlayer(uuid: SerializableUUID): OdysseyPlayerDto? = suspendTransaction {
        OdysseyPlayerTable.selectAll()
            .where { OdysseyPlayerTable.uuid eq uuid }
            .firstOrNull()
            ?: return@suspendTransaction null

        OdysseyPlayerDto(uuid, selectHomes(uuid))
    }
}
