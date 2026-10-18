package dev.slne.surf.odyssey.runtime.paper.progression.operation

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** The inventory an operation takes its items from and gives its items to. */
interface EscrowInventory {
    /**
     * Counts the items matching [ref].
     *
     * @param ref the item to count
     * @return the total amount over all matching stacks
     */
    fun count(ref: ItemRef): Int

    /**
     * Removes all [ingredients] from the inventory.
     *
     * @param ingredients the items and amounts to remove
     * @return the removed stacks, or `null` without removing anything if one of the ingredients is short
     */
    fun take(ingredients: List<Ingredient>): List<ItemStack>?

    /**
     * Adds [items] to the inventory as far as they fit.
     *
     * @param items the stacks to add
     * @return the stacks or parts of stacks that did not fit
     */
    fun give(items: List<ItemStack>): List<ItemStack>
}

/**
 * The storage slots of an online player's inventory. Must be used from the player's thread.
 *
 * Once the player is offline nothing can be taken and nothing fits.
 *
 * @param player the player whose inventory is used
 */
class PlayerEscrowInventory(private val player: Player) : EscrowInventory {
    /** Counts the matching items in the storage slots. */
    override fun count(ref: ItemRef): Int =
        player.inventory.storageContents.filter { ref.matches(it) }.sumOf { it!!.amount }

    /** Removes the ingredients from the storage slots, summing ingredients that name the same item. */
    override fun take(ingredients: List<Ingredient>): List<ItemStack>? {
        if (!player.isOnline) return null
        val needed = ingredients.groupBy { it.item }.mapValues { (_, list) -> list.sumOf { it.amount } }
        if (needed.any { (ref, amount) -> count(ref) < amount }) return null
        val inventory = player.inventory
        val taken = mutableListOf<ItemStack>()
        for ((ref, amount) in needed) {
            var missing = amount
            val contents = inventory.storageContents
            for ((slot, stack) in contents.withIndex()) {
                if (missing == 0) break
                if (stack == null || !ref.matches(stack)) continue
                if (stack.amount <= missing) {
                    missing -= stack.amount
                    taken += stack.clone()
                    inventory.setItem(slot, null)
                } else {
                    taken += stack.asQuantity(missing)
                    stack.amount -= missing
                    inventory.setItem(slot, stack)
                    missing = 0
                }
            }
        }
        return taken
    }

    /** Adds the stacks to the storage slots and returns what did not fit; returns everything if the player is offline. */
    override fun give(items: List<ItemStack>): List<ItemStack> {
        if (!player.isOnline) return items
        if (items.isEmpty()) return emptyList()
        return player.inventory.addItem(*items.map { it.clone() }.toTypedArray()).values.toList()
    }

    /**
     * Checks whether [stack] fits completely into the storage slots of the online player.
     *
     * @param stack the stack to check
     */
    fun fits(stack: ItemStack): Boolean {
        if (!player.isOnline) return false
        val max = stack.maxStackSize
        val room = player.inventory.storageContents.sumOf { slot ->
            when {
                slot == null || slot.isEmpty -> max
                slot.isSimilar(stack) -> (max - slot.amount).coerceAtLeast(0)
                else -> 0
            }
        }
        return room >= stack.amount
    }
}
