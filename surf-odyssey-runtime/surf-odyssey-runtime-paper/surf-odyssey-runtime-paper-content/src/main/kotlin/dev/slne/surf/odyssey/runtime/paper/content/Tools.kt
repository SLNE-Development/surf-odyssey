package dev.slne.surf.odyssey.runtime.paper.content

import dev.slne.surf.odyssey.runtime.paper.progression.definition.curve.Curves
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessorySlot
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolBonus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.tool
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier

/** The progression tools of the Odyssey content and the accessory slots they open. */
object Tools {
    /** Slot "Köder" of the harbour rod, opened at level 3. */
    val BAIT_SLOT: AccessorySlot = AccessorySlot("bait", text("Köder"))

    /** Slot "Schwimmer" of the harbour rod, opened at level 6. */
    val BOBBER_SLOT: AccessorySlot = AccessorySlot("bobber", text("Schwimmer"))

    /** Slot "Bohrkopf" of the pit pickaxe, opened at level 3. */
    val BIT_SLOT: AccessorySlot = AccessorySlot("bit", text("Bohrkopf"))

    /**
     * The harbour rod: shorter bite times at levels 2, 5 and 7, a chance for double catches at level 10, and the
     * bait and bobber slots at levels 3 and 6.
     */
    val HARBOR_ROD: ToolDefinition = tool("harbor_rod", ToolType.ROD) {
        name = text("Hafenrute")
        material = Material.FISHING_ROD
        craftEngineId = Key.key("odyssey", "harbor_rod")
        curve = Curves.ROD
        bonus(2, ToolBonus.BiteSpeed(5))
        bonus(5, ToolBonus.BiteSpeed(10))
        bonus(7, ToolBonus.BiteSpeed(15))
        bonus(10, ToolBonus.DoubleDrop(0.1))
        slot(3, BAIT_SLOT)
        slot(6, BOBBER_SLOT)
    }

    /**
     * The pit pickaxe: faster mining at levels 2, 5 and 8, more durability at level 4, a chance for double loot at
     * level 10, and the bit slot at level 3.
     */
    val PIT_PICKAXE: ToolDefinition = tool("pit_pickaxe", ToolType.PICKAXE) {
        name = text("Grubenhacke")
        material = Material.IRON_PICKAXE
        craftEngineId = Key.key("odyssey", "pit_pickaxe")
        curve = Curves.PICKAXE
        bonus(2, breakSpeed(0.1))
        bonus(4, ToolBonus.MaxDamage(250))
        bonus(5, breakSpeed(0.2))
        bonus(8, breakSpeed(0.3))
        bonus(10, ToolBonus.DoubleDrop(0.15))
        slot(3, BIT_SLOT)
    }

    /** All tools in the order they are registered. */
    val all: List<ToolDefinition> = listOf(HARBOR_ROD, PIT_PICKAXE)

    /**
     * Returns a bonus that raises the block break speed by [amount], scaled onto the base value.
     *
     * @param amount the scalar added to the block break speed
     */
    fun breakSpeed(amount: Double): ToolBonus =
        ToolBonus.AttributeBonus(Attribute.BLOCK_BREAK_SPEED, amount, AttributeModifier.Operation.ADD_SCALAR)
}
