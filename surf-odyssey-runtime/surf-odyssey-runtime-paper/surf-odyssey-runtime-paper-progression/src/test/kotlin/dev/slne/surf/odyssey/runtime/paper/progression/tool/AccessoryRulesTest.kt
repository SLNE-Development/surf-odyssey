package dev.slne.surf.odyssey.runtime.paper.progression.tool

import dev.slne.surf.odyssey.runtime.paper.progression.definition.curve.Curves
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.itemDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessorySlot
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.accessory
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.tool
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests for attaching accessories to and detaching them from tool slots. */
class AccessoryRulesTest {
    /** The bait slot, open from level 2. */
    private val bait = AccessorySlot("bait", text("Köder"))

    /** The reel slot, open from level 5. */
    private val reel = AccessorySlot("reel", text("Rolle"))

    /** A rod that reaches level 2 at 100 usage and level 5 at 400 usage. */
    private val rod = tool("rod", ToolType.ROD) {
        name = text("Angel")
        material = Material.FISHING_ROD
        curve = Curves.linear(100)
        slot(2, bait)
        slot(5, reel)
    }

    /** A worm that fits the bait slot. */
    private val worm = accessory(
        "bait_worm",
        itemDefinition("bait_worm") {
            displayName = text("Wurm")
            material = Material.PAPER
        },
        bait,
    )

    /** Returns a fresh rod state with [usage] and [accessories]. */
    private fun state(usage: Long, accessories: Map<String, String> = emptyMap()) =
        ToolState("rod", UUID.randomUUID(), usage, Chronicle(), accessories)

    @Test
    fun `an accessory is attached to its open empty slot`() {
        val attached = AccessoryRules.attach(rod, state(100), "bait", worm)

        assertEquals(mapOf("bait" to "bait_worm"), attached?.accessories)
    }

    @Test
    fun `an accessory is not attached to a slot that is not open yet`() {
        assertNull(AccessoryRules.attach(rod, state(99), "bait", worm))
    }

    @Test
    fun `an accessory is not attached to a slot of another kind`() {
        assertNull(AccessoryRules.attach(rod, state(400), "reel", worm))
    }

    @Test
    fun `an accessory is not attached to a filled slot`() {
        assertNull(AccessoryRules.attach(rod, state(100, mapOf("bait" to "bait_worm")), "bait", worm))
    }

    @Test
    fun `detaching empties the slot and names the accessory`() {
        val (detached, accessoryId) = checkNotNull(AccessoryRules.detach(state(100, mapOf("bait" to "bait_worm")), "bait"))

        assertEquals(emptyMap(), detached.accessories)
        assertEquals("bait_worm", accessoryId)
    }

    @Test
    fun `detaching an empty slot does nothing`() {
        assertNull(AccessoryRules.detach(state(100), "bait"))
    }
}
