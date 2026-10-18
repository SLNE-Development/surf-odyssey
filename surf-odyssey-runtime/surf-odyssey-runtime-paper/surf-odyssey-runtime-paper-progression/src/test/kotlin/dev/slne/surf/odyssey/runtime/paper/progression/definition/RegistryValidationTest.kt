package dev.slne.surf.odyssey.runtime.paper.progression.definition

import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentValidationException
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirements
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

    @Test
    fun `an npc whose visibility references its own quest by id builds`() {
        val master = TestPacks.npcNamed("harbor_master", Requirements.questCompleted("master_intro"))
        val base = TestPacks.valid()
        ProgressionRegistry.build(
            base.copy(npcs = base.npcs + master, quests = base.quests + TestPacks.quest("master_intro", giver = master)),
        )
    }

    @Test
    fun `an unknown quest id in a requirement is reported`() {
        val ghost = TestPacks.npcNamed("ghost", Requirements.not(Requirements.questStepReached("ghost_quest", 1)))
        val base = TestPacks.valid()
        val ex = assertFailsWith<ContentValidationException> {
            ProgressionRegistry.build(base.copy(npcs = base.npcs + ghost))
        }
        assertTrue(ex.problems.any { "unknown quest" in it && "ghost_quest" in it })
    }

    @Test
    fun `an unknown recipe id in a requirement is reported`() {
        val ghost = TestPacks.npcNamed("ghost", Requirements.anyOf(Requirements.recipeUnlocked("ghost_recipe")))
        val base = TestPacks.valid()
        val ex = assertFailsWith<ContentValidationException> {
            ProgressionRegistry.build(base.copy(npcs = base.npcs + ghost))
        }
        assertTrue(ex.problems.any { "unknown recipe" in it && "ghost_recipe" in it })
    }
}
