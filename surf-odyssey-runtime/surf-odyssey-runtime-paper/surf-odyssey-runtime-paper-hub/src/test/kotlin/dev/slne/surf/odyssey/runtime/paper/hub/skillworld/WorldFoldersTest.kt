package dev.slne.surf.odyssey.runtime.paper.hub.skillworld

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldFoldersTest {
    private fun withTemp(block: (Path) -> Unit) {
        val root = Files.createTempDirectory("worldfolders")
        try {
            block(root)
        } finally {
            WorldFolders.delete(root)
        }
    }

    private fun world(dir: Path, vararg files: String): Path {
        Files.createDirectories(dir)
        Files.writeString(dir.resolve("level.dat"), "level")
        files.forEach { name ->
            val file = dir.resolve(name)
            Files.createDirectories(file.parent)
            Files.writeString(file, name)
        }
        return dir
    }

    @Test
    fun `a folder with a level dat is a world`() = withTemp { root ->
        assertTrue(WorldFolders.isWorld(world(root.resolve("w"))))
        assertFalse(WorldFolders.isWorld(root.resolve("missing")))
    }

    @Test
    fun `a dimension folder with a region directory and no level dat is a world`() = withTemp { root ->
        val dimension = root.resolve("dimension")
        Files.createDirectories(dimension.resolve("region"))
        Files.writeString(dimension.resolve("region/r.0.0.mca"), "region")
        Files.writeString(dimension.resolve("paper-world.yml"), "paper")

        assertTrue(WorldFolders.isWorld(dimension))
        assertFalse(WorldFolders.isWorld(root.resolve("empty").also { Files.createDirectories(it) }))

        WorldFolders.replace(dimension, root.resolve("dimension_runtime"))

        assertEquals("region", Files.readString(root.resolve("dimension_runtime/region/r.0.0.mca")))
        assertTrue(Files.exists(root.resolve("dimension_runtime/paper-world.yml")))
    }

    @Test
    fun `replace copies nested files and skips the world identity files`() = withTemp { root ->
        val source = world(
            root.resolve("src"),
            "region/r.0.0.mca", "uid.dat", "session.lock", "data/paper/metadata.dat", "data/paper/level_overrides.dat",
        )

        WorldFolders.replace(source, root.resolve("src_runtime"))

        val target = root.resolve("src_runtime")
        assertEquals("region/r.0.0.mca", Files.readString(target.resolve("region/r.0.0.mca")))
        assertTrue(Files.exists(target.resolve("level.dat")))
        assertTrue(Files.exists(target.resolve("data/paper/level_overrides.dat")))
        assertFalse(Files.exists(target.resolve("uid.dat")))
        assertFalse(Files.exists(target.resolve("session.lock")))
        assertFalse(Files.exists(target.resolve("data/paper/metadata.dat")))
    }

    @Test
    fun `replace removes a stale runtime copy that carries the marker`() = withTemp { root ->
        val source = world(root.resolve("src"))
        WorldFolders.replace(source, root.resolve("src_runtime"))
        Files.writeString(root.resolve("src_runtime/stale.txt"), "stale")

        WorldFolders.replace(source, root.resolve("src_runtime"))

        assertFalse(Files.exists(root.resolve("src_runtime/stale.txt")))
        assertTrue(Files.exists(root.resolve("src_runtime/level.dat")))
    }

    @Test
    fun `replace refuses to delete a folder it did not create`() = withTemp { root ->
        val source = world(root.resolve("src"))
        val foreign = world(root.resolve("src_runtime"), "precious.txt")

        assertFailsWith<IllegalArgumentException> { WorldFolders.replace(source, foreign) }

        assertTrue(Files.exists(foreign.resolve("precious.txt")))
    }

    @Test
    fun `replace leaves the target untouched when the source is not a world`() = withTemp { root ->
        WorldFolders.replace(world(root.resolve("src")), root.resolve("src_runtime"))
        Files.createDirectories(root.resolve("empty"))

        assertFailsWith<IllegalArgumentException> { WorldFolders.replace(root.resolve("empty"), root.resolve("src_runtime")) }

        assertTrue(Files.exists(root.resolve("src_runtime/level.dat")))
    }

    @Test
    fun `replace rejects a target inside the source or equal to it`() = withTemp { root ->
        val source = world(root.resolve("src"))

        assertFailsWith<IllegalArgumentException> { WorldFolders.replace(source, source) }
        assertFailsWith<IllegalArgumentException> { WorldFolders.replace(source, source.resolve("inner")) }
    }

    @Test
    fun `delete ignores a missing folder`() = withTemp { root ->
        WorldFolders.delete(root.resolve("missing"))
    }
}
