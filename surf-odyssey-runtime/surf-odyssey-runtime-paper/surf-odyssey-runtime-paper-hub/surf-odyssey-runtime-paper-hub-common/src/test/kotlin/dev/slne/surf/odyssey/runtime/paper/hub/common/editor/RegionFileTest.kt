package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

/** Checks how [RegionFile] saves a session's shape block into the regions folder. */
class RegionFileTest {
    /** Returns a complete session for the region [id]. */
    private fun session(id: String) = EditorSession(id).apply {
        startCuboid(excluded = false)
        addPoint(BlockPoint(0, 0, 0))
        addPoint(BlockPoint(1, 1, 1))
    }

    /** Returns a new empty temporary directory. */
    private fun tempDir(): Path = Files.createTempDirectory("region-file-test")

    @Test
    fun `a new region is written to its own file`() {
        val dir = tempDir()
        val result = RegionFile.save(dir, session("north_lake"))
        val saved = assertIs<RegionFile.Result.Saved>(result)
        assertEquals(dir.resolve("regions").resolve("north_lake.kt"), saved.file)
        assertEquals(RegionCodeWriter.write(session("north_lake")), saved.file.readText())
    }

    @Test
    fun `an existing file is not overwritten`() {
        val dir = tempDir()
        val file = dir.resolve("regions").createDirectories().resolve("north_lake.kt")
        file.writeText("keep")
        assertIs<RegionFile.Result.Exists>(RegionFile.save(dir, session("north_lake")))
        assertEquals("keep", file.readText())
    }

    @Test
    fun `a file whose write fails is deleted`() {
        val dir = tempDir()
        val result = RegionFile.save(dir, "north_lake") {
            it.write("shape {")
            throw IOException("disk full")
        }
        assertIs<RegionFile.Result.Failed>(result)
        assertFalse(dir.resolve("regions").resolve("north_lake.kt").exists())
    }

    @Test
    fun `a write error is reported instead of thrown`() {
        val dir = tempDir()
        dir.resolve("regions").writeText("not a directory")
        assertIs<RegionFile.Result.Failed>(RegionFile.save(dir, session("north_lake")))
    }
}
