package dev.slne.surf.odyssey.runtime.microservice.scoreboard

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.upsert
import dev.slne.surf.odyssey.core.common.scoreboard.ScoreboardService
import dev.slne.surf.odyssey.runtime.microservice.db.table.ScoreboardSettingTable
import kotlinx.coroutines.flow.firstOrNull

/** Stores the scoreboard settings in the odyssey tables. */
object ScoreboardServiceImpl : ScoreboardService {
    override suspend fun isEnabled(playerUuid: SerializableUUID): Boolean = suspendTransaction {
        ScoreboardSettingTable.selectAll()
            .where { ScoreboardSettingTable.ownerUuid eq playerUuid }
            .firstOrNull()
            ?.get(ScoreboardSettingTable.enabled)
            ?: true
    }

    override suspend fun setEnabled(playerUuid: SerializableUUID, enabled: Boolean): Unit = suspendTransaction {
        ScoreboardSettingTable.upsert(ScoreboardSettingTable.ownerUuid) {
            it[ownerUuid] = playerUuid
            it[ScoreboardSettingTable.enabled] = enabled
        }
    }
}
