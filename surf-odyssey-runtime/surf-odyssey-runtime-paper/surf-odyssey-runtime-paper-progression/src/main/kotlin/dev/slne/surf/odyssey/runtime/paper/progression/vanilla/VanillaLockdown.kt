package dev.slne.surf.odyssey.runtime.paper.progression.vanilla

import dev.slne.surf.api.paper.event.register
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Turns a server into one without vanilla progression: no vanilla recipes, no vanilla loot and
 * keep inventory in every world.
 */
object VanillaLockdown {
    private val installed = AtomicBoolean(false)

    /**
     * Removes all vanilla recipes, registers the listeners that block vanilla crafting, brewing,
     * trading and loot, and turns keep inventory on in every world. Players already online forget their
     * recipes. Logs how many recipes were removed. Calling it again has no effect.
     *
     * @param plugin the plugin that owns the log
     * @param mobLoot the source of loot that mobs may still drop
     */
    fun install(plugin: JavaPlugin, mobLoot: MobLootProvider) {
        if (!installed.compareAndSet(false, true)) return
        val removed = RecipeLockdown.clearRecipes()
        plugin.logger.info("Removed $removed vanilla recipes")
        RecipeLockdown.register()
        LootLockdown(mobLoot).register()
        KeepInventory.apply()
        KeepInventory.register()
        Bukkit.getOnlinePlayers().forEach(RecipeLockdown::forgetRecipes)
    }
}
