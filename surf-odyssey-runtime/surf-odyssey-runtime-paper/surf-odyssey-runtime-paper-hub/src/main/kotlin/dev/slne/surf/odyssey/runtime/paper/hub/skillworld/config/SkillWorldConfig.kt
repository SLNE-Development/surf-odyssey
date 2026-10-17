package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config

import dev.slne.surf.api.core.config.serializer.collection.map.ThrowExceptions
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockBox
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockPos
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SkillWorldConfig(
    val worldName: String? = null,
    val portals: @ThrowExceptions Map<String, PortalConfig> = emptyMap(),
    val arrivals: Map<SkillType, LocationConfig> = emptyMap(),
    val regions: Map<SkillType, BoxConfig> = emptyMap(),
    val mining: MiningConfig = MiningConfig(),
    val mobHunting: MobHuntingConfig = MobHuntingConfig(),
    val portalBlock: PortalBlockConfig = PortalBlockConfig(),
)

enum class PortalKind { ENTRY, EXIT }

@ConfigSerializable
data class PortalConfig(
    val kind: PortalKind = PortalKind.ENTRY,
    val skill: SkillType? = null,
    val corner1: BlockPosConfig = BlockPosConfig(),
    val corner2: BlockPosConfig = BlockPosConfig(),
)

@ConfigSerializable
data class BlockPosConfig(val x: Int = 0, val y: Int = 0, val z: Int = 0) {
    fun toPos() = BlockPos(x, y, z)

    companion object {
        fun of(pos: BlockPos) = BlockPosConfig(pos.x, pos.y, pos.z)
    }
}

@ConfigSerializable
data class BoxConfig(val min: BlockPosConfig = BlockPosConfig(), val max: BlockPosConfig = BlockPosConfig()) {
    fun toBox() = BlockBox.of(min.toPos(), max.toPos())
}

@ConfigSerializable
data class LocationConfig(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
)

@ConfigSerializable
data class MiningConfig(
    val ores: Map<String, Map<String, Int>> = DEFAULT_ORES,
    val oreChance: Double = 0.05,
    val resetIntervalMinutes: Long = 60,
    val warningSeconds: List<Long> = listOf(60, 30, 10, 5),
    val unbreakableBlocks: List<String> = listOf("BARRIER", "BEDROCK"),
    val blocksPerTick: Int = 20_000,
)

val DEFAULT_ORES: Map<String, Map<String, Int>> = mapOf(
    "STONE" to mapOf(
        "COAL_ORE" to 30, "COPPER_ORE" to 20, "IRON_ORE" to 20, "REDSTONE_ORE" to 10,
        "GOLD_ORE" to 8, "LAPIS_ORE" to 6, "DIAMOND_ORE" to 4, "EMERALD_ORE" to 2,
    ),
    "DEEPSLATE" to mapOf(
        "DEEPSLATE_COAL_ORE" to 30, "DEEPSLATE_COPPER_ORE" to 20, "DEEPSLATE_IRON_ORE" to 20,
        "DEEPSLATE_REDSTONE_ORE" to 10, "DEEPSLATE_GOLD_ORE" to 8, "DEEPSLATE_LAPIS_ORE" to 6,
        "DEEPSLATE_DIAMOND_ORE" to 4, "DEEPSLATE_EMERALD_ORE" to 2,
    ),
)

fun MiningConfig.effectiveOres(): Map<String, Map<String, Int>> = ores.ifEmpty { DEFAULT_ORES }

@ConfigSerializable
data class MobHuntingConfig(
    val mobTypes: List<String> = listOf("ZOMBIE", "SKELETON"),
    val maxMobs: Int = 10,
    val respawnIntervalSeconds: Long = 10,
    val spawnPoints: List<LocationConfig> = emptyList(),
)

@ConfigSerializable
data class PortalBlockConfig(val nexoBlockId: String = "skill_portal")
