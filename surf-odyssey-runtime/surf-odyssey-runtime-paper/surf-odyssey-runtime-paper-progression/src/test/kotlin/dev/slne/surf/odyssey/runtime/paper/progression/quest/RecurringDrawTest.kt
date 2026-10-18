package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType.DAILY
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType.WEEKLY
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for the deterministic recurring quest draw. */
class RecurringDrawTest {
    private val pool = (1..5).map { TestPacks.quest("d$it", DAILY) }

    @Test
    fun `same player and period draw the same quests`() {
        val p = UUID(0x1234L, 0x5678L)
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

    @Test
    fun `pool smaller than count returns the whole pool`() {
        val d = RecurringDraw.draw(UUID(1L, 2L), DAILY, 5, pool.take(2), 4)
        assertEquals(pool.take(2).toSet(), d.toSet())
        assertEquals(2, d.size)
    }

    @Test
    fun `negative count is rejected`() {
        assertFailsWith<IllegalArgumentException> { RecurringDraw.draw(UUID(1L, 2L), DAILY, 5, pool, -1) }
    }

    @Test
    fun `types vary the draw for the same player and periods`() {
        val p = UUID(0x1234L, 0x5678L)
        val differs = (0L..20L).any {
            RecurringDraw.draw(p, DAILY, it, pool, 2).map { q -> q.id } !=
                RecurringDraw.draw(p, WEEKLY, it, pool, 2).map { q -> q.id }
        }
        assertTrue(differs)
    }
}
