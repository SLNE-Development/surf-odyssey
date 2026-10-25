package dev.slne.surf.odyssey.runtime.paper.polartest.migration

import dev.slne.surf.polarpaper.api.data.DataSections
import dev.slne.surf.polarpaper.api.migration.RawBlockEntity
import dev.slne.surf.polarpaper.api.migration.RawChunk
import dev.slne.surf.polarpaper.api.migration.RawWorld
import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks that [RemoveCobblestone] turns plain cobblestone into air and keeps every similar block. */
class RemoveCobblestoneTest {
    private class FakeChunk(override val x: Int, override val z: Int) : RawChunk {
        val blocks = HashMap<Triple<Int, Int, Int>, String>()
        override fun getBlock(x: Int, y: Int, z: Int) = blocks[Triple(x, y, z)] ?: "minecraft:air"
        override fun setBlock(x: Int, y: Int, z: Int, state: String) {
            blocks[Triple(x, y, z)] = state
        }
        override val blockEntities = mutableListOf<RawBlockEntity>()
        override val data: DataSections get() = error("not used")
    }

    private class FakeWorld(override val chunks: List<FakeChunk>) : RawWorld {
        override val minSection = -4
        override val maxSection = 19
        override fun chunk(x: Int, z: Int) = chunks.firstOrNull { it.x == x && it.z == z }
        override fun removeChunk(x: Int, z: Int) = error("not used")
        override val data: DataSections get() = error("not used")
    }

    @Test
    fun `cobblestone becomes air at the bottom, the middle and the top of the world`() {
        val chunk = FakeChunk(0, 0)
        chunk.setBlock(0, -64, 0, "minecraft:cobblestone")
        chunk.setBlock(5, 61, 5, "minecraft:cobblestone")
        chunk.setBlock(15, 319, 15, "minecraft:cobblestone")
        RemoveCobblestone.migrateRaw(FakeWorld(listOf(chunk)))
        assertEquals("minecraft:air", chunk.getBlock(0, -64, 0))
        assertEquals("minecraft:air", chunk.getBlock(5, 61, 5))
        assertEquals("minecraft:air", chunk.getBlock(15, 319, 15))
    }

    @Test
    fun `similar blocks stay as they are`() {
        val chunk = FakeChunk(-1, 2)
        val kept = listOf(
            "minecraft:mossy_cobblestone",
            "minecraft:cobblestone_slab[type=bottom,waterlogged=false]",
            "minecraft:cobblestone_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]",
            "minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]",
            "minecraft:stone",
        )
        kept.forEachIndexed { index, state -> chunk.setBlock(index, 61, 0, state) }
        RemoveCobblestone.migrateRaw(FakeWorld(listOf(chunk)))
        kept.forEachIndexed { index, state -> assertEquals(state, chunk.getBlock(index, 61, 0)) }
    }

    @Test
    fun `the migration upgrades to version 2`() {
        assertEquals(2, RemoveCobblestone.toVersion)
    }
}
