package dev.slne.surf.odyssey.runtime.microservice.home

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ResultRow
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.statements.UpsertStatement
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.deleteWhere
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.upsert
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.core.common.home.HomeService
import dev.slne.surf.odyssey.runtime.microservice.db.table.HomeTable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import net.kyori.adventure.key.Key

object HomeServiceImpl : HomeService {
    override suspend fun listHomes(playerUuid: SerializableUUID): List<Home> = suspendTransaction {
        selectHomes(playerUuid)
    }

    override suspend fun createHome(
        playerUuid: SerializableUUID,
        home: Home
    ) = upsertHome(playerUuid, home)

    override suspend fun saveHome(
        playerUuid: SerializableUUID,
        home: Home
    ) = upsertHome(playerUuid, home)

    override suspend fun deleteHome(
        playerUuid: SerializableUUID,
        home: Home
    ): Unit = suspendTransaction {
        HomeTable.deleteWhere { (HomeTable.id eq home.id) and (HomeTable.ownerUuid eq playerUuid) }
    }

    /** Inserts the home, or overwrites it when a home with the same id exists. */
    private suspend fun upsertHome(playerUuid: SerializableUUID, home: Home): Unit = suspendTransaction {
        HomeTable.upsert(HomeTable.id) { it.fill(playerUuid, home) }
    }

    private fun UpsertStatement<Long>.fill(playerUuid: SerializableUUID, home: Home) {
        this[HomeTable.id] = home.id
        this[HomeTable.ownerUuid] = playerUuid
        this[HomeTable.worldUuid] = home.worldUuid
        this[HomeTable.name] = home.name
        this[HomeTable.description] = home.description
        this[HomeTable.itemType] = home.itemType.asString()
        this[HomeTable.x] = home.x
        this[HomeTable.y] = home.y
        this[HomeTable.z] = home.z
        this[HomeTable.yaw] = home.yaw
        this[HomeTable.pitch] = home.pitch
    }
}

/** Reads every home of [playerUuid]; must run inside a transaction. */
internal suspend fun selectHomes(playerUuid: SerializableUUID): List<Home> =
    HomeTable.selectAll()
        .where { HomeTable.ownerUuid eq playerUuid }
        .map { it.toHome() }
        .toList()

private fun ResultRow.toHome() = Home(
    id = this[HomeTable.id],
    ownerUuid = this[HomeTable.ownerUuid],
    worldUuid = this[HomeTable.worldUuid],
    name = this[HomeTable.name],
    description = this[HomeTable.description],
    itemType = Key.key(this[HomeTable.itemType]),
    x = this[HomeTable.x],
    y = this[HomeTable.y],
    z = this[HomeTable.z],
    yaw = this[HomeTable.yaw],
    pitch = this[HomeTable.pitch]
)
