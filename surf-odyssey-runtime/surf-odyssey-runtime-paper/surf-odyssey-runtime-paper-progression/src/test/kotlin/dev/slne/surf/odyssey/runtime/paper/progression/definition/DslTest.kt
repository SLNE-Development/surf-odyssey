package dev.slne.surf.odyssey.runtime.paper.progression.definition

import dev.slne.surf.odyssey.runtime.paper.progression.definition.curve.Curves
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.CustomItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.MaterialRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.tool
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for the shortcuts of the definition DSL. */
class DslTest {
    @Test
    fun `ref wraps items and materials`() {
        val rice = TestPacks.item("rice", Material.WHEAT)
        assertEquals(CustomItemRef(rice), rice.ref)
        assertEquals(MaterialRef(Material.COD), Material.COD.ref)
    }

    @Test
    fun `tools default to the curve of their type`() {
        val expected = mapOf(
            ToolType.PICKAXE to Curves.PICKAXE,
            ToolType.AXE to Curves.AXE,
            ToolType.ROD to Curves.ROD,
            ToolType.SWORD to Curves.WEAPON,
            ToolType.BOW to Curves.WEAPON,
        )
        val actual = ToolType.entries.associateWith { type ->
            tool("plain_${type.name.lowercase()}", type) {
                name = text(type.name)
                material = Material.STICK
            }.curve
        }
        assertEquals(expected, actual)
    }
}
