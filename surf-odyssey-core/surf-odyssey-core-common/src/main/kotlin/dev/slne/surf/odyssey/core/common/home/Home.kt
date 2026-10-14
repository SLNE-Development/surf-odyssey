package dev.slne.surf.odyssey.core.common.home

import dev.slne.surf.api.core.generated.ItemTypeKeys
import dev.slne.surf.api.core.serializer.adventure.key.SerializableKey
import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.odyssey.core.common.player.PlayerManager
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
    /** Inserts or updates this home, keeping the owner's cached homes in sync when the owner is cached here. */
    suspend fun save() {
        val owner = PlayerManager.cachedPlayer(ownerUuid)

        if (owner != null) {
            owner.saveHome(this)
        } else {
            HomeBridge.saveHome(ownerUuid, this)
        }
    }

    /**
     * Saves a copy of this home with [edit] applied and returns it. This instance stays unchanged, so a failed save
     * leaves the cached home as it was.
     */
    suspend fun edit(edit: Home.() -> Unit): Home = copy().apply(edit).also { it.save() }

    /** Deletes this home, removing it from the owner's cached homes when the owner is cached here. */
    suspend fun delete() {
        val owner = PlayerManager.cachedPlayer(ownerUuid)

        if (owner != null) {
            owner.deleteHome(this)
        } else {
            HomeBridge.deleteHome(ownerUuid, this)
        }
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
