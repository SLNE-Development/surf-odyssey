package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import kotlin.test.Test
import kotlin.test.assertFalse

/** Checks which changes [EnvironmentRules] let alter a hub world. */
class EnvironmentRulesTest {
    @Test
    fun `no change is allowed`() {
        for (change in WorldChange.entries) {
            assertFalse(EnvironmentRules.allows(change), "$change")
        }
    }
}
