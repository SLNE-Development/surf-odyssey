package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks that every region builder stores the protection settings declared in its `settings { }` block. */
class RegionSettingsTest {
    private val regions = RegionRegistry().apply {
        mine("m") {
            shape { cuboid(0, 0, 0, 1, 1, 1) }
            settings { doors = true }
        }
        forest("f") {
            shape { cuboid(10, 0, 0, 11, 1, 1) }
            settings { levers = true }
        }
        fishing("w") {
            shape { cuboid(20, 0, 0, 21, 1, 1) }
            fish("perch", 1) { item { ItemStack.empty() } }
            settings { buttons = true }
        }
        mobSpawn("z") {
            shape { cuboid(30, 0, 0, 31, 1, 1) }
            mobs(EntityType.WOLF)
            spawnPoint(30, 0, 0)
            settings { pressurePlates = true }
        }
    }.build().associate { it.id to it.settings }

    @Test
    fun `a region without a settings block allows nothing`() {
        val region = RegionRegistry().apply { mine("plain") { shape { cuboid(0, 0, 0, 1, 1, 1) } } }.build().single()
        assertEquals(ProtectionSettings(), region.settings)
    }

    @Test
    fun `mines store their settings`() = assertEquals(protectionSettings { doors = true }, regions["m"])

    @Test
    fun `forests store their settings`() = assertEquals(protectionSettings { levers = true }, regions["f"])

    @Test
    fun `fishing regions store their settings`() = assertEquals(protectionSettings { buttons = true }, regions["w"])

    @Test
    fun `mob spawn regions store their settings`() = assertEquals(protectionSettings { pressurePlates = true }, regions["z"])
}
