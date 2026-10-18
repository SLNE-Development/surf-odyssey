package dev.slne.surf.odyssey.runtime.paper.progression.station

import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests which clicks on a station block open the station. */
class StationClicksTest {
    @Test
    fun `a main hand right click opens the station`() {
        assertTrue(StationClicks.opens(Action.RIGHT_CLICK_BLOCK, EquipmentSlot.HAND, alreadyDenied = false))
    }

    @Test
    fun `a click another listener already denied does not open the station`() {
        assertFalse(StationClicks.opens(Action.RIGHT_CLICK_BLOCK, EquipmentSlot.HAND, alreadyDenied = true))
    }

    @Test
    fun `left clicks and off hand clicks do not open the station`() {
        assertFalse(StationClicks.opens(Action.LEFT_CLICK_BLOCK, EquipmentSlot.HAND, alreadyDenied = false))
        assertFalse(StationClicks.opens(Action.RIGHT_CLICK_BLOCK, EquipmentSlot.OFF_HAND, alreadyDenied = false))
    }
}
