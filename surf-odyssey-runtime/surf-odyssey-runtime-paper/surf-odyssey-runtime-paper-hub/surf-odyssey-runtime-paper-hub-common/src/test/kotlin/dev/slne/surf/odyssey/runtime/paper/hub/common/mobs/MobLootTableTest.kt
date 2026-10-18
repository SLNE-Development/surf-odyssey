package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import org.bukkit.inventory.ItemStack
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Tests how [MobLootTable] picks the code loot of a mob region by weight. */
class MobLootTableTest {
    /** Returns an item provider that must never be called. */
    private fun unused(): () -> ItemStack = { error("not needed") }

    @Test
    fun `an empty table drops nothing`() {
        assertNull(MobLootTable(emptyList()).roll(Random(1)))
        assertTrue(MobLootTable(emptyList()).drops(Random(1)).isEmpty())
    }

    @Test
    fun `a fixed random picks the same entry every time`() {
        val bone = MobLootEntry(10, unused())
        val nothing = MobLootEntry(10, null)
        val table = MobLootTable(listOf(bone, nothing))
        assertSame(table.roll(Random(42)), table.roll(Random(42)))
    }

    @Test
    fun `weights decide how often an entry is picked`() {
        val common = MobLootEntry(90, unused())
        val rare = MobLootEntry(10, unused())
        val table = MobLootTable(listOf(common, rare))
        val picks = (0 until 10_000).count { table.roll(Random(it)) === rare }
        assertTrue(picks in 800..1200, "rare picked $picks times")
    }

    @Test
    fun `a nothing entry drops no item`() {
        val table = MobLootTable(listOf(MobLootEntry(1, null)))
        assertEquals(emptyList<ItemStack>(), table.drops(Random(3)))
    }

    @Test
    fun `entries need a positive weight`() {
        assertFailsWith<IllegalArgumentException> { MobLootEntry(0, null) }
    }

    @Test
    fun `the drops dsl collects entries in order`() {
        val table = MobLootBuilder().apply { entry(5, unused()); nothing(3) }.build()
        assertEquals(listOf(5, 3), table.entries.map { it.weight })
        assertNull(table.entries[1].item)
    }
}
