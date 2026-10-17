package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnvironmentRulesTest {
    private val natural = listOf(
        WorldChange.EXPLOSION,
        WorldChange.BURN,
        WorldChange.SPREAD,
        WorldChange.LEAF_DECAY,
        WorldChange.ENTITY_CHANGE,
    )
    private val byPlayer = listOf(
        WorldChange.PLACE,
        WorldChange.BUCKET_EMPTY,
        WorldChange.BUCKET_FILL,
        WorldChange.PLAYER_CHANGE,
    )

    @Test
    fun `natural changes are always denied, even while building`() {
        for (change in natural) {
            assertFalse(EnvironmentRules.allows(change, building = false), "$change without build mode")
            assertFalse(EnvironmentRules.allows(change, building = true), "$change in build mode")
        }
    }

    @Test
    fun `player changes are allowed only in build mode`() {
        for (change in byPlayer) {
            assertFalse(EnvironmentRules.allows(change, building = false), "$change without build mode")
            assertTrue(EnvironmentRules.allows(change, building = true), "$change in build mode")
        }
    }

    @Test
    fun `every change is covered by the tests`() {
        assertTrue((natural + byPlayer).toSet() == WorldChange.entries.toSet())
    }
}
