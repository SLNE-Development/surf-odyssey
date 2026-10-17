package dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/** Tests the hash that identifies the map of a hub world. */
class WorldHashTest {
    private val files = linkedMapOf(
        "level.dat" to byteArrayOf(1, 2, 3),
        "region/r.0.0.mca" to byteArrayOf(4, 5, 6),
        "dimensions/minecraft/overworld/region/r.0.-1.mca" to byteArrayOf(7, 8),
        "dimensions/minecraft/overworld/region/r.1.0.mca" to byteArrayOf(9),
    )

    /** Creates a new world folder holding [content], written in the order of the map. */
    private fun world(content: Map<String, ByteArray>): Path {
        val root = Files.createTempDirectory("world-hash")
        content.forEach { (path, bytes) ->
            val file = root.resolve(path)
            file.parent.createDirectories()
            file.writeBytes(bytes)
        }
        return root
    }

    @Test
    fun `the same files give the same hash`() =
        assertEquals(WorldHash.compute(world(files)), WorldHash.compute(world(files)))

    @Test
    fun `the order in which the files were written does not matter`() {
        val reversed = LinkedHashMap(files.entries.reversed().associate { it.key to it.value })
        assertEquals(WorldHash.compute(world(files)), WorldHash.compute(world(reversed)))
    }

    @Test
    fun `a changed byte in a region file changes the hash`() {
        val changed = files + ("dimensions/minecraft/overworld/region/r.1.0.mca" to byteArrayOf(10))
        assertNotEquals(WorldHash.compute(world(files)), WorldHash.compute(world(changed)))
    }

    @Test
    fun `a changed byte in level dat changes the hash`() {
        val changed = files + ("level.dat" to byteArrayOf(1, 2, 4))
        assertNotEquals(WorldHash.compute(world(files)), WorldHash.compute(world(changed)))
    }

    @Test
    fun `an added region file changes the hash`() {
        val added = files + ("region/r.5.5.mca" to byteArrayOf())
        assertNotEquals(WorldHash.compute(world(files)), WorldHash.compute(world(added)))
    }

    @Test
    fun `a renamed region file changes the hash`() {
        val renamed = files - "region/r.0.0.mca" + ("region/r.0.1.mca" to byteArrayOf(4, 5, 6))
        assertNotEquals(WorldHash.compute(world(files)), WorldHash.compute(world(renamed)))
    }

    @Test
    fun `bytes moved from one file to the next change the hash`() {
        val moved = files + mapOf(
            "dimensions/minecraft/overworld/region/r.0.-1.mca" to byteArrayOf(7),
            "dimensions/minecraft/overworld/region/r.1.0.mca" to byteArrayOf(8, 9),
        )
        assertNotEquals(WorldHash.compute(world(files)), WorldHash.compute(world(moved)))
    }

    @Test
    fun `a configured directory is the map source`() {
        val folder = world(files)
        assertEquals(folder, WorldHash.source(folder.toString()))
    }

    @Test
    fun `without a configured folder there is no map source`() = assertNull(WorldHash.source(null))

    @Test
    fun `a blank configured folder is no map source`() = assertNull(WorldHash.source(" "))

    @Test
    fun `a missing configured folder is no map source`() =
        assertNull(WorldHash.source(world(files).resolve("missing").toString()))

    @Test
    fun `a configured file is no map source`() =
        assertNull(WorldHash.source(world(files).resolve("level.dat").toString()))

    @Test
    fun `files other than level dat and region files are ignored`() {
        val other = files + mapOf(
            "session.lock" to byteArrayOf(42),
            "data/minecraft/scoreboard.dat" to byteArrayOf(43),
            "region/notes.txt" to byteArrayOf(44),
        )
        assertEquals(WorldHash.compute(world(files)), WorldHash.compute(world(other)))
    }

    @Test
    fun `an unreadable folder gives no hash and reports the error once`() {
        val failures = mutableListOf<java.io.IOException>()
        assertNull(WorldHash.computeOrNull(world(files).resolve("missing")) { failures.add(it) })
        assertEquals(1, failures.size)
    }

    @Test
    fun `a readable folder gives the same hash as compute`() {
        val folder = world(files)
        assertEquals(WorldHash.compute(folder), WorldHash.computeOrNull(folder) { error("unexpected $it") })
    }
}
