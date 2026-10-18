package dev.slne.surf.odyssey.runtime.paper.content

import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.papermc.paper.registry.RegistryAccess
import net.kyori.adventure.key.Key
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Tests that the content of the Odyssey servers forms a valid progression registry. */
class OdysseyContentTest {
    /** The registry built from the content. */
    private val registry = ProgressionRegistry.build(OdysseyContent)

    /**
     * Installs a stand-in registry access whose registries return mocks, so that registry values such as attributes
     * load without a server.
     */
    companion object {
        init {
            mockkStatic(RegistryAccess::class)
            val access = proxy(RegistryAccess::class.java) { _, _ ->
                proxy(Registry::class.java) { method, _ -> if (method.name == "getOrThrow") mockk<Attribute>() else null }
            }
            every { RegistryAccess.registryAccess() } returns access
        }

        /** Creates a proxy of [type] whose calls are answered by [answer]. */
        private fun <T> proxy(type: Class<T>, answer: (Method, Array<out Any?>?) -> Any?): T =
            type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args -> answer(method, args) })
    }

    @Test
    fun `registry builds from the content`() {
        assertNotNull(registry)
    }

    @Test
    fun `every custom item uses the craft engine item of the same id`() {
        registry.allItems.forEach { item ->
            assertEquals(Key.key("odyssey", item.id), item.craftEngineId, "item ${item.id}")
        }
    }

    @Test
    fun `the content defines the items stations recipes titles tools and accessories`() {
        assertEquals(
            listOf(
                "odyssey_book", "sushi", "smoked_fish", "fish_soup", "copper_sheet", "lantern_oil", "bait_worm",
                "drill_bit", "fish_platter",
            ),
            registry.allItems.map { it.id },
        )
        assertEquals(listOf("workbench", "cooking_station"), registry.allStations.map { it.id })
        assertEquals(
            listOf(
                "copper_sheet", "lantern_oil", "bait_worm", "drill_bit", "sushi", "smoked_fish", "fish_soup",
                "fish_platter",
            ),
            registry.allRecipes.map { it.id },
        )
        assertEquals(listOf("lightkeeper", "angler"), registry.allTitles.map { it.id })
        assertEquals(listOf("harbor_rod", "pit_pickaxe"), registry.allTools.map { it.id })
        assertEquals(listOf("bait_worm", "drill_bit"), registry.allAccessories.map { it.id })
    }
}
