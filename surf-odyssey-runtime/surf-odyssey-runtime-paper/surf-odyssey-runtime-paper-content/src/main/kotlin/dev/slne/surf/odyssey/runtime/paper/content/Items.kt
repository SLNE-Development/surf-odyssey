package dev.slne.surf.odyssey.runtime.paper.content

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.itemDefinition
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component.text
import org.bukkit.Material

/**
 * The custom items of the Odyssey content. Every item takes its model from the CraftEngine item `odyssey:<id>` and
 * falls back to the base material of that CraftEngine item.
 */
object Items {
    /** The quest book that lists the player's quests. */
    val ODYSSEY_BOOK: ItemDefinition = item("odyssey_book", Material.BOOK, "Odyssee-Buch", "Hier steht, was noch zu tun ist.")

    /** Sushi, made at the cooking station from salmon, kelp and wheat. */
    val SUSHI: ItemDefinition = item("sushi", Material.COOKED_COD, "Sushi", "Lachs, Kelp und Reis aus Weizen.")

    /** Smoked fish, made at the cooking station from cod over oak wood. */
    val SMOKED_FISH: ItemDefinition = item("smoked_fish", Material.COOKED_COD, "Räucherfisch", "Riecht nach Eiche und Hafen.")

    /** Fish soup, made at the cooking station from cod and kelp. */
    val FISH_SOUP: ItemDefinition = item("fish_soup", Material.COOKED_COD, "Fischsuppe", "Wärmt in kalten Nächten.")

    /** Copper sheet, made at the workbench from copper ingots. */
    val COPPER_SHEET: ItemDefinition = item("copper_sheet", Material.PAPER, "Kupferblech", "Dünn gehämmertes Kupfer.")

    /** Lamp oil, made at the workbench from salmon in a glass bottle. */
    val LANTERN_OIL: ItemDefinition = item("lantern_oil", Material.PAPER, "Lampenöl", "Brennt lange und hell.")

    /** Bait worm, made at the workbench; fits into the bait slot of a fishing rod. */
    val BAIT_WORM: ItemDefinition = item("bait_worm", Material.PAPER, "Köderwurm", "Fische können nicht widerstehen.")

    /** Drill bit, made at the workbench from copper sheets; fits into the bit slot of a pickaxe. */
    val DRILL_BIT: ItemDefinition = item("drill_bit", Material.PAPER, "Bohrkopf", "Frisst sich durch jedes Gestein.")

    /** Fish platter, made at the cooking station from sushi and smoked fish. */
    val FISH_PLATTER: ItemDefinition = item("fish_platter", Material.COOKED_COD, "Fischplatte", "Ein Festmahl vom Ostkai.")

    /** All items in the order they are registered. */
    val all: List<ItemDefinition> = listOf(
        ODYSSEY_BOOK, SUSHI, SMOKED_FISH, FISH_SOUP, COPPER_SHEET, LANTERN_OIL, BAIT_WORM, DRILL_BIT, FISH_PLATTER,
    )

    /**
     * Defines an item whose model is the CraftEngine item `odyssey:<id>`.
     *
     * @param id the item ID
     * @param material the base material of the CraftEngine item
     * @param name the German item name
     * @param lore the German lore line
     */
    private fun item(id: String, material: Material, name: String, lore: String): ItemDefinition =
        itemDefinition(id) {
            displayName = text(name)
            this.lore = listOf(text(lore))
            this.material = material
            craftEngineId = Key.key("odyssey", id)
        }
}
