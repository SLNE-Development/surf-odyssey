package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/** The region editor wand: a blaze rod marked as the wand in its persistent data. */
object RegionWand {
    /** Persistent data key that marks an item as the wand. */
    private val key: NamespacedKey get() = NamespacedKey(HubPlugin.instance, "region_wand")

    /** Creates a new wand. */
    fun create(): ItemStack = ItemStack(Material.BLAZE_ROD).apply {
        editMeta { meta ->
            meta.itemName(buildText { primary("Regions-Zauberstab") })
            meta.persistentDataContainer.set(key, PersistentDataType.BOOLEAN, true)
        }
    }

    /** Returns whether [item] is a wand. */
    fun isWand(item: ItemStack?): Boolean =
        item != null && item.type == Material.BLAZE_ROD && item.persistentDataContainer.has(key, PersistentDataType.BOOLEAN)
}
