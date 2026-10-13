package dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.paper.inventory.framework.view.icon.viewIcon
import me.devnatan.inventoryframework.context.Context
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound
import org.bukkit.entity.Player

fun Context.playGeneralClickSound() {
    player.playSound(true) {
        type(Sound.UI_BUTTON_CLICK)
    }
}

fun Context.playNoSound() {
    player.playNoSound()
}

fun Player.playNoSound() {
    playSound(true) {
        type(Sound.ENTITY_VILLAGER_NO)
    }
}

fun Context.playYesSound() {
    player.playYesSound()
}

fun Player.playYesSound() {
    playSound(true) {
        type(Sound.ENTITY_VILLAGER_YES)
    }
}

val backItem = viewIcon(ViewIconType.CROSS, ViewIconColor.RED) {
    displayName {
        primary("Zurück", TextDecoration.BOLD)
    }

    buildLore {
        emptyLine()
        line {
            spacer("Zum vorherigen Menü zurückkehren".toSmallCaps())
        }
        emptyLine()
    }
}

val closeItem = viewIcon(ViewIconType.CROSS, ViewIconColor.RED) {
    displayName {
        primary("Schließen", TextDecoration.BOLD)
    }

    buildLore {
        emptyLine()
        line {
            spacer("Das Menü schließen".toSmallCaps())
        }
        emptyLine()
    }
}