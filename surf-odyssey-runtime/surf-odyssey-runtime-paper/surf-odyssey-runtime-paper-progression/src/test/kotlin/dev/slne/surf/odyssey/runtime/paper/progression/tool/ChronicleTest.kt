package dev.slne.surf.odyssey.runtime.paper.progression.tool

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests for the chronicle a tool keeps: its owners, counters and catches. */
class ChronicleTest {
    /** Creates [count] distinct owners named A, B, C and so on. */
    private fun owners(count: Int) = (0 until count).map { UUID.randomUUID() to ('A' + it).toString() }

    /** Returns the plain text of [component], or `null`. */
    private fun plain(component: Component?) = component?.let(PlainTextComponentSerializer.plainText()::serialize)

    @Test
    fun `the owner chain does not repeat the last owner`() {
        val alice = UUID.randomUUID()
        val chronicle = Chronicle().withOwner(alice, "Alice").withOwner(alice, "Alice")

        assertEquals(listOf("Alice"), chronicle.owners.map { it.name })
    }

    @Test
    fun `an owner who comes back is added again`() {
        val (a, b) = owners(2)
        val chronicle = Chronicle().withOwner(a.first, a.second).withOwner(b.first, b.second)
            .withOwner(a.first, a.second)

        assertEquals(listOf("A", "B", "A"), chronicle.owners.map { it.name })
    }

    @Test
    fun `the owner line shows the owners in order`() {
        val chronicle = owners(3).fold(Chronicle()) { acc, (id, name) -> acc.withOwner(id, name) }

        assertEquals("Geführt von: A → B → C", plain(chronicle.ownerLine()))
    }

    @Test
    fun `the owner line shows at most five names with an ellipsis`() {
        val chronicle = owners(7).fold(Chronicle()) { acc, (id, name) -> acc.withOwner(id, name) }

        assertEquals(5, chronicle.owners.size)
        assertEquals("Geführt von: … → C → D → E → F → G", plain(chronicle.ownerLine()))
    }

    @Test
    fun `a chronicle without owners has no owner line`() {
        assertNull(Chronicle().ownerLine())
    }

    @Test
    fun `the biggest catch is replaced only by a bigger one`() {
        val chronicle = Chronicle()
            .record(ChronicleEvent.Catch("Hering", 30))
            .record(ChronicleEvent.Catch("Dorsch", 25))
            .record(ChronicleEvent.Catch("Lachs", 30))

        assertEquals(CatchRecord("Hering", 30), chronicle.biggestCatch)

        val bigger = chronicle.record(ChronicleEvent.Catch("Thunfisch", 120))
        assertEquals(CatchRecord("Thunfisch", 120), bigger.biggestCatch)
    }

    @Test
    fun `the first catch is kept and catches without size do not count as biggest`() {
        val chronicle = Chronicle()
            .record(ChronicleEvent.Catch("Stiefel"))
            .record(ChronicleEvent.Catch("Hering", 30))

        assertEquals(CatchRecord("Stiefel", null), chronicle.firstCatch)
        assertEquals(CatchRecord("Hering", 30), chronicle.biggestCatch)
        assertEquals(2, chronicle.catches)
    }

    @Test
    fun `mined blocks and killed mobs are counted`() {
        val chronicle = Chronicle()
            .record(ChronicleEvent.BlockMined)
            .record(ChronicleEvent.BlockMined)
            .record(ChronicleEvent.MobKilled)

        assertEquals(2, chronicle.blocksMined)
        assertEquals(1, chronicle.mobsKilled)
        assertEquals(0, chronicle.catches)
    }

    @Test
    fun `a chronicle survives the json round trip`() {
        val (a, b) = owners(2)
        val chronicle = Chronicle().withOwner(a.first, a.second).withOwner(b.first, b.second)
            .record(ChronicleEvent.Catch("Hering", 30))

        assertEquals(chronicle, Chronicle.decode(chronicle.encode()))
    }
}
