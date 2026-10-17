package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.hud

import dev.slne.surf.api.core.messages.adventure.showTitle
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

private val GREETED_LABELS = setOf(RegionLabel.MINING, RegionLabel.FISHING, RegionLabel.MOB_HUNTING)

fun welcomeUpdate(label: RegionLabel?, greeted: Set<String>): Set<String>? {
    if (label == null || label !in GREETED_LABELS || label.name in greeted) return null

    return greeted + label.name
}

fun welcomeTitleText(label: RegionLabel): String? = when (label) {
    RegionLabel.MINING -> "Willkommen in der Mining Region!"
    RegionLabel.FISHING -> "Willkommen in der Fishing Region!"
    RegionLabel.MOB_HUNTING -> "Willkommen in der Mob Hunting Region!"
    RegionLabel.HUB, RegionLabel.SKILL_WORLD -> null
}

object WelcomeTitles {
    private val key = NamespacedKey("odyssey", "greeted_regions")

    fun greet(player: Player, label: RegionLabel) {
        val container = player.persistentDataContainer
        val greeted = container.get(key, PersistentDataType.LIST.strings()).orEmpty().toSet()
        val updated = welcomeUpdate(label, greeted) ?: return
        val text = welcomeTitleText(label) ?: return

        container.set(key, PersistentDataType.LIST.strings(), updated.toList())
        player.showTitle {
            title { primary(text) }
        }
    }
}
