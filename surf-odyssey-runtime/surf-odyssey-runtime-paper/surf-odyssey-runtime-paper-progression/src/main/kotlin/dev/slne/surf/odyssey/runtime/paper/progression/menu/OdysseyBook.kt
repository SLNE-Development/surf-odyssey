package dev.slne.surf.odyssey.runtime.paper.progression.menu

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import net.momirealms.craftengine.core.util.Key as CraftEngineKey

/** An inventory move a player can try with an item. */
enum class BookMove {
    /** Dropping the item out of the inventory. */
    DROP,

    /** A plain click on a slot. */
    CLICK,

    /** A shift click, which moves the item to another inventory or section. */
    SHIFT_CLICK,

    /** A number key, which swaps a slot with a hotbar slot. */
    NUMBER_KEY,

    /** Swapping the item into the off hand. */
    OFFHAND_SWAP,

    /** Dragging the cursor item over a slot. */
    DRAG,

    /** Picking a block or entity, which swaps the picked item into a hotbar slot. */
    PICK,
}

/** Decides which inventory moves the Odyssey book blocks, so that it stays in [BOOK_SLOT]. */
object BookRules {
    /** The hotbar slot that holds the book: the last one. */
    const val BOOK_SLOT = 8

    /**
     * Returns whether [move] has to be cancelled to keep the book in its slot.
     *
     * Dropping and swapping into the off hand are blocked when they move the book. Clicks, shift clicks, number key
     * swaps, drags and picks are blocked when they move the book or touch [BOOK_SLOT] of the player's inventory.
     *
     * @param move the move
     * @param slot the slot of the player's inventory the move touches: the clicked or dragged-over slot, or the hotbar
     *        slot of a number key, or the source or target slot of a pick; `null` if the move touches no slot of the
     *        player's inventory
     * @param isBook whether the item the move takes is the Odyssey book
     */
    fun isProtectedMove(move: BookMove, slot: Int?, isBook: Boolean): Boolean = when (move) {
        BookMove.DROP, BookMove.OFFHAND_SWAP -> isBook
        BookMove.CLICK, BookMove.SHIFT_CLICK, BookMove.NUMBER_KEY, BookMove.DRAG, BookMove.PICK ->
            isBook || slot == BOOK_SLOT
    }
}

/** The Odyssey book, the fixed hotbar item that opens the Odyssey menu. */
object OdysseyBook {
    /** Persistent data key `odyssey:book` that marks the book. */
    val BOOK_KEY: NamespacedKey = NamespacedKey("odyssey", "book")

    /** The name of the client key mapping that opens the quick actions. */
    const val QUICK_ACTIONS_KEYBIND = "key.quickActions"

    /** The CraftEngine item the book is built from. */
    private val CRAFT_ENGINE_ID: CraftEngineKey = CraftEngineKey.of("odyssey", "odyssey_book")

    /** The lore lines of the book. */
    private val LORE: List<Component> = listOf(
        buildText {
            append(Component.keybind(QUICK_ACTIONS_KEYBIND, Colors.VARIABLE_KEY))
            spacer(": ")
            variableValue("Odyssey-Menü")
        },
        buildText {
            variableKey("Shift + Rechtsklick")
            spacer(": ")
            variableValue("Odyssey-Menü")
        },
        buildText {
            variableKey("Rechtsklick")
            spacer(": ")
            variableValue("Forschung (bald)")
        },
    ).map { it.decoration(TextDecoration.ITALIC, false) }

    /** Returns whether [stack] is the Odyssey book. */
    fun isBook(stack: ItemStack?): Boolean {
        if (stack == null || stack.isEmpty) return false
        return stack.persistentDataContainer.get(BOOK_KEY, PersistentDataType.BOOLEAN) == true
    }

    /**
     * Builds a new book from the CraftEngine item `odyssey:odyssey_book`, or from a plain book named "Odyssey-Buch"
     * when CraftEngine does not know the item.
     */
    fun create(): ItemStack {
        val stack = CraftEngineItems.byId(CRAFT_ENGINE_ID)?.buildBukkitItem() ?: ItemStack(Material.BOOK).apply {
            editMeta { it.itemName(buildText { text("Odyssey-Buch") }) }
        }
        stack.amount = 1
        stack.editMeta { meta ->
            meta.lore(LORE)
            meta.persistentDataContainer.set(BOOK_KEY, PersistentDataType.BOOLEAN, true)
        }
        return stack
    }

    /**
     * Puts a fresh book into [BookRules.BOOK_SLOT] of [player] and removes every other book from the inventory and
     * the cursor. An item that was in the book slot is moved into the inventory, or dropped at the player's feet if
     * the inventory is full.
     */
    fun ensure(player: Player) {
        val inventory = player.inventory
        for (slot in 0 until inventory.size) {
            if (slot != BookRules.BOOK_SLOT && isBook(inventory.getItem(slot))) inventory.setItem(slot, null)
        }
        if (isBook(player.itemOnCursor)) player.setItemOnCursor(null)
        val displaced = inventory.getItem(BookRules.BOOK_SLOT)?.takeUnless { it.isEmpty || isBook(it) }
        inventory.setItem(BookRules.BOOK_SLOT, create())
        if (displaced != null) {
            inventory.addItem(displaced).values.forEach { player.world.dropItem(player.location, it) }
        }
    }
}
