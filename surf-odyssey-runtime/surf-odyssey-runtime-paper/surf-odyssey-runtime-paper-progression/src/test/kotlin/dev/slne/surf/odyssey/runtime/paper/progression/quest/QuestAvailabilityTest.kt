package dev.slne.surf.odyssey.runtime.paper.progression.quest

import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.FakeContext
import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ProgressionRegistry
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestStateView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirements
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for which quests an npc offers. */
class QuestAvailabilityTest {
    private val player = UUID.fromString("3f1e9c2a-7b44-4d0e-9a51-2c6d8e0f4b17")
    private val now = Instant.parse("2026-03-10T12:00:00Z")
    private val today = Periods.index(QuestType.DAILY, now)

    /** Returns the IDs of the quests Brina offers to the player of [ctx] now, looked up in [registry]. */
    private fun offered(registry: ProgressionRegistry, ctx: FakeContext): List<String> =
        QuestAvailability.offeredBy(TestPacks.brina, ctx.progression, ctx, player, now, registry).map { it.id }

    @Test
    fun `main quests are offered once`() {
        val registry = ProgressionRegistry.build(TestPacks.valid())

        assertTrue("harbor_intro" in offered(registry, FakeContext()))
        val completed = FakeContext(quests = mapOf("harbor_intro" to QuestStateView(QuestStatus.COMPLETED, 0, 0)))
        assertFalse("harbor_intro" in offered(registry, completed))
        val active = FakeContext(quests = mapOf("harbor_intro" to QuestStateView(QuestStatus.ACTIVE, 0, 0)))
        assertFalse("harbor_intro" in offered(registry, active))
    }

    @Test
    fun `only drawn recurring quests are offered`() {
        val registry = ProgressionRegistry.build(TestPacks.valid(dailyCount = 2))
        val drawn = RecurringDraw.draw(player, QuestType.DAILY, today, registry.pool(QuestType.DAILY), 2).map { it.id }

        val dailies = offered(registry, FakeContext()).filter { it.startsWith("d") }

        assertEquals(2, drawn.size)
        assertEquals(drawn.toSet(), dailies.toSet())
        assertEquals(listOf("w1", "m1"), offered(registry, FakeContext()).filter { it == "w1" || it == "m1" })
    }

    @Test
    fun `recurring quests accepted in this period are not offered again`() {
        val registry = ProgressionRegistry.build(TestPacks.valid(dailyCount = 2))
        val drawn = RecurringDraw.draw(player, QuestType.DAILY, today, registry.pool(QuestType.DAILY), 2).first().id

        val thisPeriod = FakeContext(quests = mapOf(drawn to QuestStateView(QuestStatus.COMPLETED, 0, today)))
        assertFalse(drawn in offered(registry, thisPeriod))
        val lastPeriod = FakeContext(quests = mapOf(drawn to QuestStateView(QuestStatus.COMPLETED, 0, today - 1)))
        assertTrue(drawn in offered(registry, lastPeriod))
    }

    @Test
    fun `requirements hide quests`() {
        val base = TestPacks.valid()
        val gated = TestPacks.quest("net_repair", requirement = Requirements.questCompleted("harbor_intro"))
        val registry = ProgressionRegistry.build(base.copy(quests = base.quests + gated))

        assertFalse("net_repair" in offered(registry, FakeContext()))
        val done = FakeContext(quests = mapOf("harbor_intro" to QuestStateView(QuestStatus.COMPLETED, 0, 0)))
        assertTrue("net_repair" in offered(registry, done))
    }

    @Test
    fun `abandoned side quests are offered again`() {
        val base = TestPacks.valid()
        val side = TestPacks.quest("net_repair")
        val registry = ProgressionRegistry.build(base.copy(quests = base.quests + side))

        val abandoned = FakeContext(quests = mapOf("net_repair" to QuestStateView(QuestStatus.ABANDONED, 0, 0)))
        assertTrue("net_repair" in offered(registry, abandoned))
    }

    @Test
    fun `active quests at an npc are those whose current step names it`() {
        val other = TestPacks.npcNamed("koch")
        val base = TestPacks.valid()
        val registry = ProgressionRegistry.build(base.copy(npcs = base.npcs + other))
        val ctx = FakeContext(
            quests = mapOf(
                "harbor_intro" to QuestStateView(QuestStatus.ACTIVE, 0, 0),
                "d1" to QuestStateView(QuestStatus.ACTIVE, 0, today - 1),
                "d2" to QuestStateView(QuestStatus.ACTIVE, 0, today),
                "w1" to QuestStateView(QuestStatus.COMPLETED, 0, 0),
            )
        )

        assertEquals(
            listOf("harbor_intro", "d2"),
            QuestAvailability.activeAt(TestPacks.brina, ctx.progression, now, registry).map { it.id }
        )
        assertTrue(QuestAvailability.activeAt(other, ctx.progression, now, registry).isEmpty())
    }
}
