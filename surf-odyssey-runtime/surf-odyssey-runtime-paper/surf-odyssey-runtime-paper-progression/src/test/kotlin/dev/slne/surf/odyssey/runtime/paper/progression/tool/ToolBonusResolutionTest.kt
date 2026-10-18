package dev.slne.surf.odyssey.runtime.paper.progression.tool

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.itemDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessorySlot
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolBonus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.accessory
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.tool
import io.mockk.mockk
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for adding up the bonuses of the levels of a tool and of its accessories. */
class ToolBonusResolutionTest {
    /** Stand-in for a registry attribute, which cannot be loaded without a server. */
    private val speed = mockk<Attribute>()

    /** The bait slot of the rod. */
    private val baitSlot = AccessorySlot("bait", text("Köder"))

    /** A rod with bonuses at levels 1, 3 and 5. */
    private val rod = tool("rod", ToolType.ROD) {
        name = text("Angel")
        material = Material.FISHING_ROD
        bonus(1, ToolBonus.BiteSpeed(5))
        bonus(3, ToolBonus.BiteSpeed(10), ToolBonus.MaxDamage(20))
        bonus(3, ToolBonus.AttributeBonus(speed, 0.5, AttributeModifier.Operation.ADD_NUMBER))
        bonus(5, ToolBonus.DoubleDrop(0.1), ToolBonus.MaxDamage(30))
        slot(3, baitSlot)
    }

    /** A worm bait that shortens the bite time and doubles drops more often. */
    private val worm = accessory(
        "bait_worm",
        itemDefinition("bait_worm") {
            displayName = text("Wurm")
            material = Material.PAPER
        },
        baitSlot,
    ) {
        bonus(ToolBonus.BiteSpeed(15), ToolBonus.DoubleDrop(0.05))
    }

    @Test
    fun `bonuses accumulate up to the current level`() {
        val bonuses = ToolBonuses.resolve(rod, level = 4, accessories = emptyList())

        assertEquals(15, bonuses.biteSpeedPercent)
        assertEquals(20, bonuses.extraMaxDamage)
        assertEquals(0.0, bonuses.doubleDropChance)
        assertEquals(
            listOf(ToolBonus.AttributeBonus(speed, 0.5, AttributeModifier.Operation.ADD_NUMBER)),
            bonuses.attributes,
        )
    }

    @Test
    fun `level 1 has only the bonuses of level 1`() {
        val bonuses = ToolBonuses.resolve(rod, level = 1, accessories = emptyList())

        assertEquals(5, bonuses.biteSpeedPercent)
        assertEquals(0, bonuses.extraMaxDamage)
        assertEquals(emptyList(), bonuses.attributes)
    }

    @Test
    fun `accessory bonuses are added to the level bonuses`() {
        val bonuses = ToolBonuses.resolve(rod, level = 5, accessories = listOf(worm))

        assertEquals(30, bonuses.biteSpeedPercent)
        assertEquals(50, bonuses.extraMaxDamage)
        assertEquals(0.15, bonuses.doubleDropChance, 1e-9)
    }

    @Test
    fun `bite speed and double drop chance are capped`() {
        val bonuses = ToolBonuses.resolve(rod, level = 5, accessories = List(10) { worm })

        assertEquals(100, bonuses.biteSpeedPercent)
        assertEquals(0.6, bonuses.doubleDropChance, 1e-9)

        val capped = ToolBonuses.resolve(rod, level = 5, accessories = List(30) { worm })
        assertEquals(1.0, capped.doubleDropChance, 1e-9)
    }

    @Test
    fun `the maximum damage is the base plus the extra durability`() {
        assertEquals(84, ToolBonuses.maxDamageFor(base = 64, extra = 20, damage = 10))
    }

    @Test
    fun `the maximum damage stays above the current damage`() {
        assertEquals(71, ToolBonuses.maxDamageFor(base = 64, extra = 0, damage = 70))
        assertEquals(65, ToolBonuses.maxDamageFor(base = 64, extra = 0, damage = 64))
    }
}
