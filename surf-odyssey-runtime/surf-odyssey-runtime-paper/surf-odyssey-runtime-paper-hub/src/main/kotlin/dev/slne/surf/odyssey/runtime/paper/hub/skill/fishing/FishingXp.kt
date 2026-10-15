package dev.slne.surf.odyssey.runtime.paper.hub.skill.fishing

import dev.slne.surf.api.core.rarity.Rarity

object FishingXp {
    fun forRarity(rarity: Rarity): Long = when (rarity) {
        Rarity.COMMON -> 5L
        Rarity.UNCOMMON -> 8L
        Rarity.RARE -> 15L
        Rarity.EPIC -> 30L
        Rarity.LEGENDARY -> 60L
        Rarity.MYTHIC -> 120L
    }
}
