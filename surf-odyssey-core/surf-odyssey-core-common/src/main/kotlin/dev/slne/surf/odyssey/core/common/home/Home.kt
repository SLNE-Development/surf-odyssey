package dev.slne.surf.odyssey.core.common.home

import dev.slne.surf.api.core.generated.ItemTypeKeys
import dev.slne.surf.api.core.serializer.adventure.key.SerializableKey
import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import kotlinx.serialization.Serializable

@Serializable
data class Home(
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

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false
        if (yaw != other.yaw) return false
        if (pitch != other.pitch) return false
        if (ownerUuid != other.ownerUuid) return false
        if (worldUuid != other.worldUuid) return false
        if (name != other.name) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + yaw.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + ownerUuid.hashCode()
        result = 31 * result + worldUuid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        return result
    }


}
