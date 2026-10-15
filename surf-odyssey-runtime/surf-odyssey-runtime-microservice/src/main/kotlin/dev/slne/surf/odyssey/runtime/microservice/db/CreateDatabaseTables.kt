package dev.slne.surf.odyssey.runtime.microservice.db

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.odyssey.runtime.microservice.db.table.HomeTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.SkillTable

/** Creates the odyssey tables that do not exist yet. */
object CreateDatabaseTables {
    suspend fun create() = suspendTransaction {
        SchemaUtils.create(
            OdysseyPlayerTable,
            HomeTable,
            SkillTable
        )
    }
}
