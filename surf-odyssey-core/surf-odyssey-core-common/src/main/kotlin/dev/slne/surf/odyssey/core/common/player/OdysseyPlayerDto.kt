package dev.slne.surf.odyssey.core.common.player

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.odyssey.core.common.home.Home
import kotlinx.serialization.Serializable

@Serializable
data class OdysseyPlayerDto(
    val uuid: SerializableUUID,
    val homes: List<Home>
)