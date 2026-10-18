package dev.slne.surf.odyssey.runtime.paper.progression.definition

import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for the listing accessors of the registry. */
class RegistryLookupTest {
    @Test
    fun `all accessors list the pack content in order`() {
        val pack = TestPacks.valid()
        val registry = ProgressionRegistry.build(pack)
        assertEquals(
            listOf(
                pack.items, pack.stations, pack.recipes, pack.titles, pack.npcs, pack.quests, pack.tools,
                pack.accessories,
            ),
            listOf(
                registry.allItems, registry.allStations, registry.allRecipes, registry.allTitles, registry.allNpcs,
                registry.allQuests, registry.allTools, registry.allAccessories,
            ),
        )
    }
}
