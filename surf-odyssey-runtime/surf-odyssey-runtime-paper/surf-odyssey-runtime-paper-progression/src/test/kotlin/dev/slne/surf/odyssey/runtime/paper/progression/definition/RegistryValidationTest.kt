package dev.slne.surf.odyssey.runtime.paper.progression.definition

import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentValidationException
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Tests for the startup validation of content packs. */
class RegistryValidationTest {
    @Test
    fun `duplicate ids are reported together with missing nodes`() {
        val ex = assertFailsWith<ContentValidationException> { ProgressionRegistry.build(TestPacks.broken()) }
        assertTrue(ex.problems.any { "duplicate" in it && "sushi" in it })
        assertTrue(ex.problems.any { "missing node" in it && "brina_nope" in it })
    }

    @Test
    fun `a valid pack builds`() {
        ProgressionRegistry.build(TestPacks.valid())
    }

    @Test
    fun `a pool smaller than its draw count is rejected`() {
        assertFailsWith<ContentValidationException> { ProgressionRegistry.build(TestPacks.valid(dailyCount = 5)) }
    }
}
