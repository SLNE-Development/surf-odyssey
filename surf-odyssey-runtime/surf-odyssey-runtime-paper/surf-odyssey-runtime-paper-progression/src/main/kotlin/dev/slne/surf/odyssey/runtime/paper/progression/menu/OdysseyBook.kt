package dev.slne.surf.odyssey.runtime.paper.progression.menu

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationRunner
import dev.slne.surf.odyssey.runtime.paper.progression.operation.StashResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.concurrent.ConcurrentHashMap
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
     * Removes every book outside [BookRules.BOOK_SLOT] from the inventory and the cursor of [player] and puts a fresh
     * book into that slot if it is empty or already holds the book. Another item in the slot is left alone.
     *
     * @return `true` if the slot now holds the book, `false` if another item occupies it
     */
    fun placeIfFree(player: Player): Boolean {
        val inventory = player.inventory
        for (slot in 0 until inventory.size) {
            if (slot != BookRules.BOOK_SLOT && isBook(inventory.getItem(slot))) inventory.setItem(slot, null)
        }
        if (isBook(player.itemOnCursor)) player.setItemOnCursor(null)
        val current = inventory.getItem(BookRules.BOOK_SLOT)
        if (current != null && !current.isEmpty && !isBook(current)) return false
        inventory.setItem(BookRules.BOOK_SLOT, create())
        return true
    }

    /**
     * Does what [placeIfFree] does and, if another item occupies [BookRules.BOOK_SLOT], makes room for the book:
     * the item moves into a free storage slot, or, when none is free, into the pickup box through
     * [OperationRunner.stash].
     *
     * Once the pickup box stored the item, the book is put into the slot if it is still empty and the player is told.
     * When the pickup box failed and the item was dropped at the player's feet, the slot stays empty so that the item
     * can be picked up again. When the item was lost, it is put back into the slot if that is still empty. Only one
     * stash per player runs at a time; nothing happens while no engine is installed.
     */
    fun ensure(player: Player) {
        if (placeIfFree(player)) return
        val inventory = player.inventory
        val free = inventory.storageContents.indices.firstOrNull { slot ->
            slot != BookRules.BOOK_SLOT && inventory.getItem(slot).let { it == null || it.isEmpty }
        }
        if (free == null) {
            stashOccupant(player)
            return
        }
        inventory.setItem(free, inventory.getItem(BookRules.BOOK_SLOT))
        inventory.setItem(BookRules.BOOK_SLOT, create())
    }

    /** The players whose book slot item is being put into the pickup box. */
    private val stashing: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /** The source stored with the deliveries of book slot items. */
    private const val STASH_SOURCE = "book_slot"

    /** Takes the item out of the book slot of [player] and puts it into the pickup box, as described in [ensure]. */
    private fun stashOccupant(player: Player) {
        val runtime = Progression.runtimeOrNull ?: return
        if (!stashing.add(player.uniqueId)) return
        val inventory = player.inventory
        val occupant = inventory.getItem(BookRules.BOOK_SLOT)?.takeUnless { it.isEmpty }?.clone()
        if (occupant == null) {
            stashing.remove(player.uniqueId)
            return
        }
        inventory.setItem(BookRules.BOOK_SLOT, null)
        val plugin = ProgressionModule.plugin
        plugin.launch(plugin.entityDispatcher(player)) {
            try {
                when (runtime.operations.stash(player, listOf(occupant), STASH_SOURCE)) {
                    StashResult.STORED -> if (slotEmpty(player)) {
                        inventory.setItem(BookRules.BOOK_SLOT, create())
                        ProgressionMessages.bookSlotStashed(player)
                    }
                    StashResult.DROPPED -> plugin.logger.warning(
                        "Dropped the book slot item ${occupant.type.name} of ${player.uniqueId} at their feet"
                    )
                    StashResult.LOST -> if (slotEmpty(player)) {
                        inventory.setItem(BookRules.BOOK_SLOT, occupant)
                        plugin.logger.warning("Put the book slot item of ${player.uniqueId} back after a failed stash")
                    } else {
                        plugin.logger.severe("Lost the book slot item ${occupant.type.name} of ${player.uniqueId}")
                    }
                }
            } finally {
                stashing.remove(player.uniqueId)
            }
        }
    }

    /** Returns whether the book slot of [player] is empty. */
    private fun slotEmpty(player: Player): Boolean =
        player.inventory.getItem(BookRules.BOOK_SLOT).let { it == null || it.isEmpty }
}
