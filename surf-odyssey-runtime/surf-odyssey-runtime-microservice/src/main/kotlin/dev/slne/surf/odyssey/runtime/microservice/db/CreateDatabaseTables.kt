package dev.slne.surf.odyssey.runtime.microservice.db

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.odyssey.runtime.microservice.db.table.HomeTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.TradeItemTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.TradeRequestTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.TradeSettingTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.TradeTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.DeliveryTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.OperationTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.QuestCounterTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.QuestStateTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.StationJobTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.TitleTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.ToolSightingTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.ToolTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.TrackedQuestTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.progression.UnlockedRecipeTable

/** Creates the odyssey tables that do not exist yet. */
object CreateDatabaseTables {
    suspend fun create() = suspendTransaction {
        SchemaUtils.create(
            OdysseyPlayerTable,
            HomeTable,
            TradeTable,
            TradeItemTable,
            TradeRequestTable,
            TradeSettingTable,
            QuestStateTable,
            QuestCounterTable,
            UnlockedRecipeTable,
            TitleTable,
            TrackedQuestTable,
            DeliveryTable,
            StationJobTable,
            OperationTable,
            ToolTable,
            ToolSightingTable
        )
    }
}
