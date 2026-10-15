package dev.slne.surf.odyssey.runtime.paper.hub.skill.mobhunting

import org.bukkit.entity.EntityType

object MobHuntingXp {
    private val values: Map<EntityType, Long> = mapOf(
        EntityType.ZOMBIE to 5, EntityType.SKELETON to 5 // Add more entity types and their corresponding XP values as needed
    ).mapValues { it.value.toLong() }

    fun forEntity(type: EntityType): Long? = values[type]
}
