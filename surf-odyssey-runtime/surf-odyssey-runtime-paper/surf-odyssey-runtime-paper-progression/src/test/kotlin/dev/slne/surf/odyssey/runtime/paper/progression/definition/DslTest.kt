package dev.slne.surf.odyssey.runtime.paper.progression.definition

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.CustomItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.MaterialRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
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
}
