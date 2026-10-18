package dev.slne.surf.odyssey.runtime.paper.progression.definition.item

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/** Reference to either a custom item or a plain vanilla material, used for ingredients, prices and rewards. */
sealed interface ItemRef {
    /** Name of the referenced item for display in menus and dialogs. */
    val displayName: Component

    /**
     * Checks whether [stack] is the referenced item.
     *
     * @param stack the stack to check, may be `null`
     */
    fun matches(stack: ItemStack?): Boolean

    /**
     * Creates a stack of the referenced item.
     *
     * @param amount stack size
     */
    fun create(amount: Int): ItemStack
}

/**
 * Reference to a custom item.
 *
 * @property item the referenced custom item
 */
data class CustomItemRef(val item: ItemDefinition) : ItemRef {
    /** The display name of [item]. */
    override val displayName: Component
        get() = item.displayName

    /** Matches stacks created from [item]. */
    override fun matches(stack: ItemStack?): Boolean = item.matches(stack)

    /** Creates a stack of [item]. */
    override fun create(amount: Int): ItemStack = item.create(amount)
}

/**
 * Reference to a plain vanilla material.
 *
 * @property material the referenced material
 */
data class MaterialRef(val material: Material) : ItemRef {
    /** The translated vanilla name of [material]. */
    override val displayName: Component
        get() = Component.translatable(material)

    /** Matches stacks of [material] that are not custom items. */
    override fun matches(stack: ItemStack?): Boolean =
        stack != null && stack.type == material && ItemDefinition.itemIdOf(stack) == null

    /** Creates a plain stack of [material]. */
    override fun create(amount: Int): ItemStack = ItemStack.of(material, amount)
}

/**
 * An item and an amount, used for recipe ingredients and item prices.
 *
 * @property item the required item
 * @property amount how many of the item are required
 */
data class Ingredient(val item: ItemRef, val amount: Int)
