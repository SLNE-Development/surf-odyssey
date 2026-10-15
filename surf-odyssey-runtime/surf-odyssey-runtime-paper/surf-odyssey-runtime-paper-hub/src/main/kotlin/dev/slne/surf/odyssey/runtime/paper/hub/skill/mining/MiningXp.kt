package dev.slne.surf.odyssey.runtime.paper.hub.skill.mining

import org.bukkit.Material

object MiningXp {
    private val values: Map<Material, Long> = mapOf(
        Material.STONE to 2, Material.COBBLESTONE to 2 // Add more blocks and their XP values as needed
    ).mapValues { it.value.toLong() }

    fun forBlock(type: Material): Long? = values[type]
}