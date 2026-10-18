package dev.slne.surf.odyssey.runtime.microservice.progression

import java.util.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for the duplicate tool detection rule. */
class DuplicateRuleTest {
    private val a = UUID.randomUUID(); private val b = UUID.randomUUID()
    @Test fun `a single hand-over is not suspicious`() =
        assertFalse(DuplicateRule.isSuspected(listOf(Sighting(a, 0), Sighting(b, 10_000)), 20_000))
    @Test fun `switching back and forth within two minutes is suspicious`() =
        assertTrue(DuplicateRule.isSuspected(listOf(Sighting(a, 0), Sighting(b, 10_000), Sighting(a, 20_000)), 30_000))
    @Test fun `old sightings are ignored`() =
        assertFalse(DuplicateRule.isSuspected(listOf(Sighting(a, 0), Sighting(b, 10_000), Sighting(a, 20_000)), 200_000))
}
