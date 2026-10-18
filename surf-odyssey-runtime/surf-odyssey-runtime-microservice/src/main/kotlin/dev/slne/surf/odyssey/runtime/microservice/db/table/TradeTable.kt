package dev.slne.surf.odyssey.runtime.microservice.db.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.columns.time.offsetDateTime
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable

/** Every finished trade between two players, completed or cancelled. Kept when a player is deleted. */
object TradeTable : AuditableTable("odyssey_trades") {
    val id = nativeUuid("id")
    val firstUuid = nativeUuid("first_uuid").index()
    val secondUuid = nativeUuid("second_uuid").index()
    val outcome = varchar("outcome", 16)
    val cancelType = varchar("cancel_type", 16).nullable()
    val cancelledBy = nativeUuid("cancelled_by").nullable()
    val startedAt = offsetDateTime("started_at")
    val endedAt = offsetDateTime("ended_at")

    override val primaryKey = PrimaryKey(id)
}

/** The items offered in a trade, one row per occupied offer slot. */
object TradeItemTable : AuditableTable("odyssey_trade_items") {
    val tradeId = nativeUuid("trade_id")
        .references(TradeTable.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val ownerUuid = nativeUuid("owner_uuid").index()
    val slot = integer("slot")
    val item = largeText("item_stack")

    override val primaryKey = PrimaryKey(tradeId, ownerUuid, slot)
}

/** Every resolved trade request. Kept when a player is deleted. */
object TradeRequestTable : AuditableTable("odyssey_trade_requests") {
    val id = nativeUuid("id")
    val senderUuid = nativeUuid("sender_uuid").index()
    val receiverUuid = nativeUuid("receiver_uuid").index()
    val status = varchar("status", 16)
    val sentAt = offsetDateTime("sent_at")
    val resolvedAt = offsetDateTime("resolved_at")

    override val primaryKey = PrimaryKey(id)
}

/** Whether a player accepts incoming trade requests. Players without a row accept them. */
object TradeSettingTable : AuditableTable("odyssey_trade_settings") {
    val ownerUuid = nativeUuid("owner_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)
    val receiveRequests = bool("receive_requests").default(true)

    override val primaryKey = PrimaryKey(ownerUuid)
}
