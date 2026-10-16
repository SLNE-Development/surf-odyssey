package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.hud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WelcomeTitlesTest {
    @Test
    fun `first entry into a skill region shows the title and remembers the region`() {
        assertEquals(setOf("MINING"), welcomeUpdate(RegionLabel.MINING, emptySet()))
    }

    @Test
    fun `a region that was greeted before shows nothing`() {
        assertNull(welcomeUpdate(RegionLabel.MINING, setOf("MINING")))
    }

    @Test
    fun `another region shows the title and keeps the old ones`() {
        assertEquals(setOf("MINING", "FISHING"), welcomeUpdate(RegionLabel.FISHING, setOf("MINING")))
    }

    @Test
    fun `hub, skill world and no label never show a title`() {
        assertNull(welcomeUpdate(RegionLabel.HUB, emptySet()))
        assertNull(welcomeUpdate(RegionLabel.SKILL_WORLD, emptySet()))
        assertNull(welcomeUpdate(null, emptySet()))
    }

    @Test
    fun `title names the region with the word region`() {
        assertEquals("Willkommen in der Mining Region!", welcomeTitleText(RegionLabel.MINING))
        assertEquals("Willkommen in der Fishing Region!", welcomeTitleText(RegionLabel.FISHING))
        assertEquals("Willkommen in der Mob Hunting Region!", welcomeTitleText(RegionLabel.MOB_HUNTING))
    }

    @Test
    fun `hub and skill world have no title text`() {
        assertNull(welcomeTitleText(RegionLabel.HUB))
        assertNull(welcomeTitleText(RegionLabel.SKILL_WORLD))
    }

    @Test
    fun `unknown stored names are kept and do not break the decision`() {
        assertEquals(setOf("OLD", "MOB_HUNTING"), welcomeUpdate(RegionLabel.MOB_HUNTING, setOf("OLD")))
    }
}
