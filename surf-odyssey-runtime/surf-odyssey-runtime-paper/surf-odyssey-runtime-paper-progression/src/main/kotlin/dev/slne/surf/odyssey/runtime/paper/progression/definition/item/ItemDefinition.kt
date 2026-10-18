package dev.slne.surf.odyssey.runtime.paper.progression.definition.item

import dev.slne.surf.odyssey.runtime.paper.progression.definition.Definition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.ProgressionDsl
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import net.momirealms.craftengine.core.util.Key as CraftEngineKey

/**
 * A custom item of the progression content.
 *
 * Stacks created from this definition carry the persistent data key [ITEM_ID_KEY] with the value [id], which is how
 * [matches] recognises them.
 *
 * @property id stable item ID
 * @property displayName name shown on the item
 * @property lore lore lines shown on the item
 * @property material base material used when no CraftEngine item is set or the CraftEngine item is not loaded
 * @property craftEngineId ID of the CraftEngine item that provides the model, or `null` for a plain material item
 */
class ItemDefinition(
    override val id: String,
    val displayName: Component,
    val lore: List<Component>,
    val material: Material,
    val craftEngineId: Key?,
) : Definition {
    /**
     * Creates a stack of this item.
     *
     * The stack is built by CraftEngine when [craftEngineId] is set and CraftEngine knows the item, otherwise from
     * [material]. The display name and lore are applied and [ITEM_ID_KEY] is set to [id].
     *
     * @param amount stack size
     * @return the new stack
     */
    fun create(amount: Int = 1): ItemStack {
        val stack = craftEngineId?.let(::buildCraftEngineStack) ?: ItemStack.of(material)
        stack.amount = amount
        stack.editMeta { meta ->
            meta.itemName(displayName)
            if (lore.isNotEmpty()) meta.lore(lore)
            meta.persistentDataContainer.set(ITEM_ID_KEY, PersistentDataType.STRING, id)
        }
        return stack
    }

    /**
     * Checks whether [stack] was created from this definition.
     *
     * @param stack the stack to check, may be `null`
     * @return `true` if the stack carries [ITEM_ID_KEY] with the value [id]
     */
    fun matches(stack: ItemStack?): Boolean = itemIdOf(stack) == id

    /** Returns [id] for log and validation messages. */
    override fun toString(): String = "ItemDefinition($id)"

    /** Holds the persistent data key that marks custom item stacks. */
    companion object {
        /** Persistent data key `odyssey:item_id` holding the item ID of a custom item stack. */
        val ITEM_ID_KEY: NamespacedKey = NamespacedKey("odyssey", "item_id")

        /**
         * Reads the custom item ID of [stack].
         *
         * @return the value of [ITEM_ID_KEY], or `null` if the stack is `null`, empty or not a custom item
         */
        fun itemIdOf(stack: ItemStack?): String? {
            if (stack == null || stack.isEmpty) return null
            return stack.persistentDataContainer.get(ITEM_ID_KEY, PersistentDataType.STRING)
        }

        /**
         * Builds a single CraftEngine item stack for [key].
         *
         * @return the stack, or `null` if CraftEngine does not know the item
         */
        private fun buildCraftEngineStack(key: Key): ItemStack? =
            CraftEngineItems.byId(CraftEngineKey.of(key.namespace(), key.value()))?.buildBukkitItem()
    }
}

/**
 * Builds the values of an [ItemDefinition].
 *
 * @property id the item ID
 */
@ProgressionDsl
class ItemDefinitionBuilder(private val id: String) {
    /** Name shown on the item; required. */
    var displayName: Component? = null

    /** Lore lines shown on the item. */
    var lore: List<Component> = emptyList()

    /** Base material; required. */
    var material: Material? = null

    /** CraftEngine item that provides the model, or `null` for a plain material item. */
    var craftEngineId: Key? = null

    /**
     * Creates the definition.
     *
     * @throws IllegalStateException if [displayName] or [material] is missing
     */
    fun build(): ItemDefinition = ItemDefinition(
        id = id,
        displayName = checkNotNull(displayName) { "item $id needs a displayName" },
        lore = lore,
        material = checkNotNull(material) { "item $id needs a material" },
        craftEngineId = craftEngineId,
    )
}

/**
 * Defines a custom item.
 *
 * @param id stable item ID
 * @param block sets the values of the item
 * @return the item definition
 */
fun itemDefinition(id: String, block: ItemDefinitionBuilder.() -> Unit): ItemDefinition =
    ItemDefinitionBuilder(id).apply(block).build()
