package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.block.Action
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks which changes [EnvironmentRules] let alter a world and how it classifies them. */
class EnvironmentRulesTest {
    @Test
    fun `no change is allowed in the hub world`() {
        for (change in WorldChange.entries) {
            assertFalse(EnvironmentRules.allows(change, inHubWorld = true), "$change")
        }
    }

    @Test
    fun `other worlds are left alone`() {
        for (change in WorldChange.entries) {
            assertTrue(EnvironmentRules.allows(change, inHubWorld = false), "$change")
        }
    }

    @Test
    fun `stepping on farmland tramples it, other physical contact does not`() {
        assertTrue(EnvironmentRules.isTrample(Action.PHYSICAL, Material.FARMLAND))
        assertFalse(EnvironmentRules.isTrample(Action.PHYSICAL, Material.STONE_PRESSURE_PLATE))
        assertFalse(EnvironmentRules.isTrample(Action.RIGHT_CLICK_BLOCK, Material.FARMLAND))
    }

    @Test
    fun `item frames, paintings and armor stands are decorations`() {
        for (type in listOf(EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME, EntityType.PAINTING, EntityType.ARMOR_STAND)) {
            assertTrue(EnvironmentRules.isDecoration(type), "$type")
        }
        assertFalse(EnvironmentRules.isDecoration(EntityType.ZOMBIE))
        assertFalse(EnvironmentRules.isDecoration(EntityType.PLAYER))
    }
}
