package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionIndex
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.block.Action
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Checks which changes [EnvironmentRules] let alter a world and how it classifies them. */
class EnvironmentRulesTest {
    private val none = ProtectionSettings()

    @Test
    fun `by default nothing is allowed in the hub world`() {
        for (category in ProtectionCategory.entries) {
            assertFalse(EnvironmentRules.allows(category, inHubWorld = true, world = none, region = null), "$category")
            assertFalse(EnvironmentRules.allows(category, inHubWorld = true, world = none, region = none), "$category")
        }
    }

    @Test
    fun `other worlds are left alone`() {
        for (category in ProtectionCategory.entries) {
            assertTrue(EnvironmentRules.allows(category, inHubWorld = false, world = none, region = null), "$category")
        }
    }

    @Test
    fun `a world setting allows its category everywhere`() {
        val world = protectionSettings { doors = true }
        assertTrue(EnvironmentRules.allows(ProtectionCategory.DOORS, inHubWorld = true, world = world, region = null))
        assertTrue(EnvironmentRules.allows(ProtectionCategory.DOORS, inHubWorld = true, world = world, region = none))
        assertFalse(EnvironmentRules.allows(ProtectionCategory.LEVERS, inHubWorld = true, world = world, region = null))
    }

    @Test
    fun `a region setting allows its category only inside the region`() {
        val index = RegionIndex(
            RegionRegistry().apply {
                mine("north_mine") {
                    shape { cuboid(0, 0, 0, 9, 9, 9) }
                    settings { buttons = true }
                }
            }.build(),
        )
        val inside = index.regionAt(5, 5, 5)?.settings
        val outside = index.regionAt(50, 5, 5)?.settings
        assertTrue(EnvironmentRules.allows(ProtectionCategory.BUTTONS, inHubWorld = true, world = none, region = inside))
        assertFalse(EnvironmentRules.allows(ProtectionCategory.BUTTONS, inHubWorld = true, world = none, region = outside))
        assertFalse(EnvironmentRules.allows(ProtectionCategory.DOORS, inHubWorld = true, world = none, region = inside))
    }

    @Test
    fun `a region cannot forbid what the world allows`() {
        val world = protectionSettings { explosions = true }
        assertTrue(EnvironmentRules.allows(ProtectionCategory.EXPLOSIONS, inHubWorld = true, world = world, region = none))
    }

    @Test
    fun `each setting allows exactly its own category`() {
        val settings = listOf(
            protectionSettings { blockPlace = true } to ProtectionCategory.BLOCK_PLACE,
            protectionSettings { buckets = true } to ProtectionCategory.BUCKETS,
            protectionSettings { explosions = true } to ProtectionCategory.EXPLOSIONS,
            protectionSettings { fire = true } to ProtectionCategory.FIRE,
            protectionSettings { spread = true } to ProtectionCategory.SPREAD,
            protectionSettings { form = true } to ProtectionCategory.FORM,
            protectionSettings { fade = true } to ProtectionCategory.FADE,
            protectionSettings { fluidFlow = true } to ProtectionCategory.FLUID_FLOW,
            protectionSettings { pistons = true } to ProtectionCategory.PISTONS,
            protectionSettings { grow = true } to ProtectionCategory.GROW,
            protectionSettings { fertilize = true } to ProtectionCategory.FERTILIZE,
            protectionSettings { leafDecay = true } to ProtectionCategory.LEAF_DECAY,
            protectionSettings { trample = true } to ProtectionCategory.TRAMPLE,
            protectionSettings { entityBlockChanges = true } to ProtectionCategory.ENTITY_BLOCK_CHANGES,
            protectionSettings { hangingEntities = true } to ProtectionCategory.HANGING_ENTITIES,
            protectionSettings { armorStands = true } to ProtectionCategory.ARMOR_STANDS,
            protectionSettings { signs = true } to ProtectionCategory.SIGNS,
            protectionSettings { dispensers = true } to ProtectionCategory.DISPENSERS,
            protectionSettings { decoBlocks = true } to ProtectionCategory.DECO_BLOCKS,
            protectionSettings { doors = true } to ProtectionCategory.DOORS,
            protectionSettings { buttons = true } to ProtectionCategory.BUTTONS,
            protectionSettings { levers = true } to ProtectionCategory.LEVERS,
            protectionSettings { pressurePlates = true } to ProtectionCategory.PRESSURE_PLATES,
        )
        assertEquals(ProtectionCategory.entries.toSet(), settings.map { it.second }.toSet())
        for ((setting, own) in settings) {
            for (category in ProtectionCategory.entries) {
                assertEquals(category == own, category.allowedIn(setting), "$own enables $category")
            }
        }
    }

    @Test
    fun `stepping on farmland tramples it, other physical contact does not`() {
        assertTrue(EnvironmentRules.isTrample(Action.PHYSICAL, Material.FARMLAND))
        assertFalse(EnvironmentRules.isTrample(Action.PHYSICAL, Material.STONE_PRESSURE_PLATE))
        assertFalse(EnvironmentRules.isTrample(Action.RIGHT_CLICK_BLOCK, Material.FARMLAND))
    }

    @Test
    fun `frames and paintings are hanging entities, armor stands are their own category`() {
        for (type in listOf(EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME, EntityType.PAINTING)) {
            assertEquals(ProtectionCategory.HANGING_ENTITIES, EnvironmentRules.decorationCategory(type), "$type")
        }
        assertEquals(ProtectionCategory.ARMOR_STANDS, EnvironmentRules.decorationCategory(EntityType.ARMOR_STAND))
        assertNull(EnvironmentRules.decorationCategory(EntityType.ZOMBIE))
        assertNull(EnvironmentRules.decorationCategory(EntityType.PLAYER))
    }

    @Test
    fun `fire spreading counts as fire, other spreading as spread`() {
        assertEquals(ProtectionCategory.FIRE, EnvironmentRules.spreadCategory(Material.FIRE))
        assertEquals(ProtectionCategory.FIRE, EnvironmentRules.spreadCategory(Material.SOUL_FIRE))
        assertEquals(ProtectionCategory.SPREAD, EnvironmentRules.spreadCategory(Material.GRASS_BLOCK))
        assertEquals(ProtectionCategory.SPREAD, EnvironmentRules.spreadCategory(Material.VINE))
    }
}
