package dev.slne.surf.odyssey.runtime.microservice.trade

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.upsert
import dev.slne.surf.odyssey.core.common.trade.TradeRecord
import dev.slne.surf.odyssey.core.common.trade.TradeRequestRecord
import dev.slne.surf.odyssey.core.common.trade.TradeService
import dev.slne.surf.odyssey.runtime.microservice.db.table.TradeItemTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.TradeRequestTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.TradeSettingTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.TradeTable
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Stores trade history and trade settings in the odyssey tables. */
object TradeServiceImpl : TradeService {
    override suspend fun isReceivingRequests(playerUuid: SerializableUUID): Boolean = suspendTransaction {
        TradeSettingTable.selectAll()
            .where { TradeSettingTable.ownerUuid eq playerUuid }
            .firstOrNull()
            ?.get(TradeSettingTable.receiveRequests)
            ?: true
    }

    override suspend fun setReceivingRequests(playerUuid: SerializableUUID, enabled: Boolean): Unit =
        suspendTransaction {
            TradeSettingTable.upsert(TradeSettingTable.ownerUuid) {
                it[ownerUuid] = playerUuid
                it[receiveRequests] = enabled
            }
        }

    override suspend fun saveTrade(record: TradeRecord): Unit = suspendTransaction {
        TradeTable.upsert(TradeTable.id) {
            it[id] = record.id
            it[firstUuid] = record.firstUuid
            it[secondUuid] = record.secondUuid
            it[outcome] = record.outcome.name
            it[cancelType] = record.cancelType?.name
            it[cancelledBy] = record.cancelledBy
            it[startedAt] = record.startedAtMillis.toOffsetDateTime()
            it[endedAt] = record.endedAtMillis.toOffsetDateTime()
        }

        saveItems(record.id, record.firstUuid, record.firstItems)
        saveItems(record.id, record.secondUuid, record.secondItems)
    }

    override suspend fun saveRequest(record: TradeRequestRecord): Unit = suspendTransaction {
        TradeRequestTable.upsert(TradeRequestTable.id) {
            it[id] = record.id
            it[senderUuid] = record.senderUuid
            it[receiverUuid] = record.receiverUuid
            it[status] = record.status.name
            it[sentAt] = record.createdAtMillis.toOffsetDateTime()
            it[resolvedAt] = record.resolvedAtMillis.toOffsetDateTime()
        }
    }

    /** Stores the offered [items] of [owner], one row per item; must run inside a transaction. */
    private suspend fun saveItems(tradeId: SerializableUUID, owner: SerializableUUID, items: List<String>) {
        items.forEachIndexed { index, encoded ->
            TradeItemTable.upsert(TradeItemTable.tradeId, TradeItemTable.ownerUuid, TradeItemTable.slot) {
                it[this.tradeId] = tradeId
                it[ownerUuid] = owner
                it[slot] = index
                it[item] = encoded
            }
        }
    }

    private fun Long.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)
}
