package dev.slne.surf.odyssey.runtime.paper.progression.vanilla

import org.bukkit.Bukkit
import org.bukkit.GameRules
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldLoadEvent

/** Keeps the inventory and experience of players who die, in every loaded world and every world loaded later. */
internal object KeepInventory : Listener {

    /** Turns the keep inventory game rule on in every world that is already loaded. */
    fun apply() {
        Bukkit.getWorlds().forEach(::enable)
    }

    /** Turns the keep inventory game rule on in a world that has just loaded. */
    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        enable(event.world)
    }

    /** Turns the keep inventory game rule on in [world]. */
    private fun enable(world: World) {
        world.setGameRule(GameRules.KEEP_INVENTORY, true)
    }
}
