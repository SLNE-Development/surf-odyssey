package dev.slne.surf.odyssey.runtime.paper.common.trade

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

internal fun Player.hasRoomFor(items: List<ItemStack>): Boolean {
    val storage = inventory.storageContents.map { it?.clone() }.toTypedArray()

    for (item in items) {
        var remaining = item.amount

        for (slot in storage) {
            if (remaining <= 0) break
            if (slot == null || !slot.isSimilar(item)) continue
            val moved = minOf(remaining, slot.maxStackSize - slot.amount)
            slot.amount += moved
            remaining -= moved
        }

        while (remaining > 0) {
            val free = storage.indexOfFirst { it == null }
            if (free < 0) return false
            val moved = minOf(remaining, item.maxStackSize)
            storage[free] = item.clone().also { it.amount = moved }
            remaining -= moved
        }
    }

    return true
}

internal fun Player.giveOrDrop(items: List<ItemStack>) {
    if (items.isEmpty()) return

    inventory.addItem(*items.toTypedArray()).values.forEach { leftover ->
        world.dropItemNaturally(location, leftover)
    }
}
