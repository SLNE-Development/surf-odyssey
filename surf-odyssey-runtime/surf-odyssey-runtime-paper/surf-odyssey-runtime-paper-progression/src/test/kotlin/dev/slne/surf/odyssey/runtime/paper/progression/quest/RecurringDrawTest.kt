package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType.DAILY
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for the deterministic recurring quest draw. */
class RecurringDrawTest {
    private val pool = (1..5).map { TestPacks.quest("d$it", DAILY) }

    @Test
    fun `same player and period draw the same quests`() {
        val p = UUID.randomUUID()
        assertEquals(
            RecurringDraw.draw(p, DAILY, 10, pool, 2),
            RecurringDraw.draw(p, DAILY, 10, pool.reversed(), 2)
        )
    }

    @Test
    fun `draw returns distinct quests of the requested size`() {
        val d = RecurringDraw.draw(UUID.randomUUID(), DAILY, 3, pool, 2)
        assertEquals(2, d.toSet().size)
    }

    @Test
    fun `periods vary the draw`() {
        val p = UUID.randomUUID()
        assertTrue((0L..20L).map { RecurringDraw.draw(p, DAILY, it, pool, 2).map { q -> q.id } }.toSet().size > 1)
    }
}
