package dev.slne.surf.odyssey.runtime.paper.polartest.data

import dev.slne.surf.odyssey.runtime.paper.polartest.layout.BlockPos
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Checks the marked positions section type and the registry the handler captures from and restores into. */
class MarkedPositionsTest {
    private val ref = ChunkRef(UUID.randomUUID(), -1, 2)

    @AfterTest
    fun clear() = MarkedPositionsRegistry.clear()

    @Test
    fun `the section has its own key`() {
        assertEquals("polartest:marked-positions", MarkedPositions.key.asString())
    }

    @Test
    fun `positions survive a round trip through the section serializer`() {
        val positions = listOf(BlockPos(1, 61, 2), BlockPos(-14, 61, 34))
        val encoded = Json.encodeToString(MarkedPositions.serializer, positions)
        assertEquals(positions, Json.decodeFromString(MarkedPositions.serializer, encoded))
    }

    @Test
    fun `the registry stores, returns and releases positions per chunk`() {
        val positions = listOf(BlockPos(1, 61, 2))
        MarkedPositionsRegistry.set(ref, positions)
        assertEquals(positions, MarkedPositionsRegistry.get(ref))
        assertNull(MarkedPositionsRegistry.get(ref.copy(x = 0)))
        MarkedPositionsRegistry.remove(ref)
        assertNull(MarkedPositionsRegistry.get(ref))
    }
}
