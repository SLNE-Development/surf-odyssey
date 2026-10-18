package dev.slne.surf.odyssey.runtime.paper.progression

import dev.slne.surf.api.core.messages.adventure.sendText
import org.bukkit.entity.Player

/** The chat messages the progression engine sends to players. */
object ProgressionMessages {
    /** Tells [player] that a feature cannot be used while the progression service is unreachable. */
    fun unavailable(player: Player) = player.sendText {
        appendErrorPrefix()
        error("Das ist gerade nicht verfügbar.")
    }

    /** Tells [player] that items needed for an action are missing. */
    fun missingItems(player: Player) = player.sendText {
        appendErrorPrefix()
        error("Dir fehlen noch Gegenstände.")
    }

    /** Tells [player] that items did not fit into the inventory and wait in the pickup box. */
    fun leftInPickupBox(player: Player) = player.sendText {
        appendInfoPrefix()
        info("Nicht alles passte in dein Inventar. Den Rest findest du im Abholfach.")
    }

    /** Tells [player] that [count] deliveries wait in the pickup box. */
    fun deliveriesWaiting(player: Player, count: Int) = player.sendText {
        appendInfoPrefix()
        info("Im Abholfach ")
        variableValue(if (count == 1) "wartet 1 Lieferung" else "warten $count Lieferungen")
        info(" auf dich.")
    }

    /** Tells [player] that the inventory has no room for a delivery. */
    fun inventoryFull(player: Player) = player.sendText {
        appendErrorPrefix()
        error("Dein Inventar ist voll.")
    }

    /** Tells [player] that a delivery was already picked up. */
    fun deliveryGone(player: Player) = player.sendText {
        appendErrorPrefix()
        error("Diese Lieferung wurde bereits abgeholt.")
    }

    /** Tells [player] that the item of a delivery could not be restored. */
    fun deliveryBroken(player: Player) = player.sendText {
        appendErrorPrefix()
        error("Diese Lieferung konnte nicht geladen werden.")
    }
}
