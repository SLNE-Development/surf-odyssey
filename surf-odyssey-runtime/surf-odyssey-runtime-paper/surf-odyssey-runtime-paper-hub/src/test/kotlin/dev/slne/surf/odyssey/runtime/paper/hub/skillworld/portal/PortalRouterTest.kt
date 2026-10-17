package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal

import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.PortalKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortalRouterTest {
    private val arrivals = setOf(SkillType.MINING, SkillType.FISHING)

    @Test
    fun `entry portal leads to the arrival of its skill`() {
        assertEquals(
            PortalOutcome.Teleport(PortalTarget.Arrival(SkillType.MINING)),
            PortalRouter.route(PortalKind.ENTRY, SkillType.MINING, true, emptySet(), arrivals),
        )
    }

    @Test
    fun `entry portal is closed while the world is not loaded`() {
        assertEquals(PortalOutcome.WorldUnavailable, PortalRouter.route(PortalKind.ENTRY, SkillType.MINING, false, emptySet(), arrivals))
    }

    @Test
    fun `entry portal is closed while its skill is locked`() {
        assertEquals(
            PortalOutcome.SkillLocked,
            PortalRouter.route(PortalKind.ENTRY, SkillType.MINING, true, setOf(SkillType.MINING), arrivals),
        )
    }

    @Test
    fun `a lock of another skill does not close the portal`() {
        assertEquals(
            PortalOutcome.Teleport(PortalTarget.Arrival(SkillType.FISHING)),
            PortalRouter.route(PortalKind.ENTRY, SkillType.FISHING, true, setOf(SkillType.MINING), arrivals),
        )
    }

    @Test
    fun `entry portal without arrival or skill goes nowhere`() {
        assertEquals(PortalOutcome.NoArrival, PortalRouter.route(PortalKind.ENTRY, SkillType.MOB_HUNTING, true, emptySet(), arrivals))
        assertEquals(PortalOutcome.NoArrival, PortalRouter.route(PortalKind.ENTRY, null, true, emptySet(), arrivals))
    }

    @Test
    fun `exit portal always leads to the hub spawn`() {
        assertEquals(PortalOutcome.Teleport(PortalTarget.HubSpawn), PortalRouter.route(PortalKind.EXIT, null, true, setOf(SkillType.MINING), arrivals))
    }

    @Test
    fun `entry portals belong to the hub and exit portals to the skill world`() {
        assertTrue(PortalRouter.expectedWorldIsHub(PortalKind.ENTRY))
        assertFalse(PortalRouter.expectedWorldIsHub(PortalKind.EXIT))
    }
}
