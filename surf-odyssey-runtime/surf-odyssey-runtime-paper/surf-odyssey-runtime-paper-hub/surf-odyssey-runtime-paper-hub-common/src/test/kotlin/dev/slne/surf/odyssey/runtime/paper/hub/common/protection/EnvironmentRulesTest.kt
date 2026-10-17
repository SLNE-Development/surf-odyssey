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
            protectionSettings { redstoneComponents = true } to ProtectionCategory.REDSTONE_COMPONENTS,
            protectionSettings { utilityBlocks = true } to ProtectionCategory.UTILITY_BLOCKS,
            protectionSettings { plantHarvest = true } to ProtectionCategory.PLANT_HARVEST,
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

    @Test
    fun `right-clicking doors, trapdoors and fence gates is a door use`() {
        for (material in listOf(Material.OAK_DOOR, Material.IRON_DOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_FENCE_GATE)) {
            assertEquals(ProtectionCategory.DOORS, EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, material, null), "$material")
        }
    }

    @Test
    fun `right-clicking buttons and levers uses them`() {
        assertEquals(ProtectionCategory.BUTTONS, EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, Material.STONE_BUTTON, null))
        assertEquals(ProtectionCategory.BUTTONS, EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, Material.OAK_BUTTON, null))
        assertEquals(ProtectionCategory.LEVERS, EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, Material.LEVER, null))
    }

    @Test
    fun `stepping on a pressure plate triggers it`() {
        for (material in listOf(Material.STONE_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE, Material.LIGHT_WEIGHTED_PRESSURE_PLATE)) {
            assertEquals(ProtectionCategory.PRESSURE_PLATES, EnvironmentRules.blockUse(Action.PHYSICAL, material, null), "$material")
        }
        assertNull(EnvironmentRules.blockUse(Action.PHYSICAL, Material.FARMLAND, null))
    }

    @Test
    fun `right-clicking decoration blocks changes them`() {
        val deco = listOf(
            Material.FLOWER_POT, Material.POTTED_POPPY, Material.CAKE, Material.CANDLE_CAKE, Material.RED_CANDLE_CAKE,
            Material.CANDLE, Material.BLUE_CANDLE, Material.COMPOSTER, Material.CHISELED_BOOKSHELF, Material.DECORATED_POT,
        )
        for (material in deco) {
            assertEquals(ProtectionCategory.DECO_BLOCKS, EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, material, null), "$material")
        }
    }

    @Test
    fun `putting a book on a lectern changes it, reading it does not`() {
        assertEquals(ProtectionCategory.DECO_BLOCKS, EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, Material.LECTERN, Material.WRITTEN_BOOK))
        assertEquals(ProtectionCategory.DECO_BLOCKS, EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, Material.LECTERN, Material.WRITABLE_BOOK))
        assertNull(EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, Material.LECTERN, null))
        assertNull(EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, Material.LECTERN, Material.STICK))
    }

    @Test
    fun `an interaction is a trample or a block use`() {
        assertEquals(ProtectionCategory.TRAMPLE, EnvironmentRules.interactionCategory(Action.PHYSICAL, Material.FARMLAND, null))
        assertEquals(ProtectionCategory.DOORS, EnvironmentRules.interactionCategory(Action.RIGHT_CLICK_BLOCK, Material.OAK_DOOR, null))
        assertEquals(ProtectionCategory.PRESSURE_PLATES, EnvironmentRules.interactionCategory(Action.PHYSICAL, Material.OAK_PRESSURE_PLATE, null))
        assertNull(EnvironmentRules.interactionCategory(Action.LEFT_CLICK_BLOCK, Material.STONE, null))
    }

    @Test
    fun `changing redstone components is a redstone use`() {
        val rightClick = listOf(
            Material.NOTE_BLOCK, Material.REPEATER, Material.COMPARATOR, Material.DAYLIGHT_DETECTOR,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
        )
        for (material in rightClick) {
            assertEquals(ProtectionCategory.REDSTONE_COMPONENTS, EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, material, null), "$material")
        }
        for (material in listOf(Material.TRIPWIRE, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE)) {
            assertEquals(ProtectionCategory.REDSTONE_COMPONENTS, EnvironmentRules.blockUse(Action.PHYSICAL, material, null), "$material")
        }
        assertNull(EnvironmentRules.blockUse(Action.LEFT_CLICK_BLOCK, Material.REDSTONE_ORE, null))
    }

    @Test
    fun `using utility blocks is a utility use`() {
        val utility = listOf(
            Material.JUKEBOX, Material.BELL, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CAULDRON,
            Material.WATER_CAULDRON, Material.LAVA_CAULDRON, Material.POWDER_SNOW_CAULDRON, Material.BEEHIVE,
            Material.BEE_NEST, Material.RESPAWN_ANCHOR, Material.END_PORTAL_FRAME,
        )
        for (material in utility) {
            assertEquals(ProtectionCategory.UTILITY_BLOCKS, EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, material, null), "$material")
        }
    }

    @Test
    fun `picking berries, tilting dripleaves and trampling eggs is a plant harvest`() {
        for (material in listOf(Material.SWEET_BERRY_BUSH, Material.CAVE_VINES, Material.CAVE_VINES_PLANT)) {
            assertEquals(ProtectionCategory.PLANT_HARVEST, EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, material, null), "$material")
        }
        for (material in listOf(Material.BIG_DRIPLEAF, Material.TURTLE_EGG)) {
            assertEquals(ProtectionCategory.PLANT_HARVEST, EnvironmentRules.blockUse(Action.PHYSICAL, material, null), "$material")
        }
    }

    @Test
    fun `left-clicks and ordinary blocks are no block use`() {
        assertNull(EnvironmentRules.blockUse(Action.LEFT_CLICK_BLOCK, Material.OAK_DOOR, null))
        assertNull(EnvironmentRules.blockUse(Action.LEFT_CLICK_BLOCK, Material.FLOWER_POT, null))
        assertNull(EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, Material.STONE, null))
        assertNull(EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, Material.OAK_SIGN, null))
        assertNull(EnvironmentRules.blockUse(Action.RIGHT_CLICK_BLOCK, Material.STONE_PRESSURE_PLATE, null))
    }
}
