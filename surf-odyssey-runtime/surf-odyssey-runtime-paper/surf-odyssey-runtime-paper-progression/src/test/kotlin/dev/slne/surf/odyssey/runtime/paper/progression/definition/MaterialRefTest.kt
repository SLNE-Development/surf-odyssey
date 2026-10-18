package dev.slne.surf.odyssey.runtime.paper.progression.definition

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.MaterialRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for matching stacks against plain material references. */
class MaterialRefTest {
    /** Returns a stand-in stack of [material] that is a tool item when [tool] is `true`. */
    private fun stack(material: Material, tool: Boolean = false): ItemStack {
        val pdc = mockk<PersistentDataContainer>()
        every { pdc.get(ItemDefinition.ITEM_ID_KEY, PersistentDataType.STRING) } returns null
        every { pdc.has(ToolDefinition.TOOL_ID_KEY, PersistentDataType.STRING) } returns tool
        val stack = mockk<ItemStack>()
        every { stack.type } returns material
        every { stack.isEmpty } returns false
        every { stack.persistentDataContainer } returns pdc
        return stack
    }

    @Test
    fun `a plain stack of the material matches`() {
        assertTrue(MaterialRef(Material.FISHING_ROD).matches(stack(Material.FISHING_ROD)))
    }

    @Test
    fun `a stack of another material does not match`() {
        assertFalse(MaterialRef(Material.FISHING_ROD).matches(stack(Material.IRON_PICKAXE)))
    }

    @Test
    fun `a tool item of the material does not match`() {
        assertFalse(MaterialRef(Material.FISHING_ROD).matches(stack(Material.FISHING_ROD, tool = true)))
    }
}
