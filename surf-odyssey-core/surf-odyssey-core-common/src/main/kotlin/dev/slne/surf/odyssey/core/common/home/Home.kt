package dev.slne.surf.odyssey.core.common.home

import dev.slne.surf.api.core.generated.ItemTypeKeys
import dev.slne.surf.api.core.serializer.adventure.key.SerializableKey
import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Home(
    val id: SerializableUUID = UUID.randomUUID(),
    val ownerUuid: SerializableUUID,
    val worldUuid: SerializableUUID,
    var name: String,
    var description: String,
    val itemType: SerializableKey = ItemTypeKeys.NAME_TAG,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
) {
    suspend fun save() {
        HomeBridge.saveHome(ownerUuid, this)
    }

    suspend fun edit(edit: Home.() -> Unit): Home {
        edit()
        save()
        return this
    }

    suspend fun delete() {
        HomeBridge.deleteHome(ownerUuid, this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Home

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


}
