package dev.slne.surf.odyssey.runtime.paper.progression.vanilla

import com.destroystokyo.paper.event.inventory.PrepareGrindstoneEvent
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.BrewEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.BrewerInventory
import org.bukkit.inventory.Recipe

/**
 * Removes the vanilla recipes and blocks every crafting result that does not come from a recipe
 * registered outside the vanilla namespace.
 */
internal object RecipeLockdown : Listener {

    /**
     * Removes every registered recipe.
     *
     * @return the number of recipes that were removed
     */
    fun clearRecipes(): Int {
        var count = 0
        val iterator = Bukkit.recipeIterator()
        while (iterator.hasNext()) {
            iterator.next()
            count++
        }
        Bukkit.clearRecipes()
        return count
    }

    /** Whether [recipe] was registered outside the vanilla namespace. */
    private fun isCustom(recipe: Recipe?): Boolean =
        recipe is Keyed && recipe.key.namespace != "minecraft"

    /** Makes a joining player forget every discovered recipe. */
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        player.undiscoverRecipes(player.discoveredRecipes)
    }

    /** Empties the result of a crafting grid unless the recipe is custom. */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        if (!isCustom(event.recipe)) event.inventory.result = null
    }

    /** Empties the result of a smithing table unless the recipe is custom. */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPrepareSmithing(event: PrepareSmithingEvent) {
        if (!isCustom(event.inventory.recipe)) event.result = null
    }

    /** Empties the result of a grindstone. */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPrepareGrindstone(event: PrepareGrindstoneEvent) {
        event.result = null
    }

    /** Empties the result of an anvil that combines two items; renaming a single item stays possible. */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val second = event.inventory.secondItem
        if (second != null && !second.isEmpty) event.result = null
    }

    /** Cancels brewing. */
    @EventHandler(ignoreCancelled = true)
    fun onBrew(event: BrewEvent) {
        event.isCancelled = true
    }

    /** Cancels clicks that would put items into a brewing stand. */
    @EventHandler(ignoreCancelled = true)
    fun onBrewerClick(event: InventoryClickEvent) {
        if (event.view.topInventory !is BrewerInventory) return
        val intoBrewer = event.clickedInventory is BrewerInventory ||
            event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY
        if (intoBrewer) event.isCancelled = true
    }

    /** Cancels drags that would put items into a brewing stand. */
    @EventHandler(ignoreCancelled = true)
    fun onBrewerDrag(event: InventoryDragEvent) {
        if (event.view.topInventory !is BrewerInventory) return
        val topSize = event.view.topInventory.size
        if (event.rawSlots.any { it < topSize }) event.isCancelled = true
    }

    /** Cancels hoppers and droppers moving items into a brewing stand. */
    @EventHandler(ignoreCancelled = true)
    fun onBrewerMove(event: InventoryMoveItemEvent) {
        if (event.destination is BrewerInventory) event.isCancelled = true
    }
}
