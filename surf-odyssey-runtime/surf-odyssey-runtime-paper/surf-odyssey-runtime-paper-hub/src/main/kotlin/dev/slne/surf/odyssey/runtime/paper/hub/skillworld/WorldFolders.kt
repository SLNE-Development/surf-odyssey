package dev.slne.surf.odyssey.runtime.paper.hub.skillworld

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object WorldFolders {
    const val MARKER = ".skill-world-runtime"

    private val EXCLUDED_FILES = setOf("uid.dat", "session.lock")
    private val EXCLUDED_PATHS = setOf("data/paper/metadata.dat")

    fun isWorld(dir: Path): Boolean = Files.isRegularFile(dir.resolve("level.dat")) || Files.isDirectory(dir.resolve("region"))

    fun replace(source: Path, target: Path) {
        val from = source.toAbsolutePath().normalize()
        val to = target.toAbsolutePath().normalize()
        require(isWorld(from)) { "$from is not a world folder" }
        require(!to.startsWith(from)) { "$to must not be the source world or inside it" }
        require(!Files.exists(to) || Files.exists(to.resolve(MARKER))) { "$to exists and was not created as a runtime copy, refusing to delete it" }

        delete(to)
        Files.createDirectories(to)
        Files.writeString(to.resolve(MARKER), "")
        copyContents(from, to)
    }

    fun delete(dir: Path) {
        if (!Files.exists(dir)) return

        Files.walk(dir).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
    }

    private fun copyContents(source: Path, target: Path) {
        Files.walk(source).use { paths ->
            paths.forEach { path ->
                val relative = source.relativize(path)
                val relativePath = relative.joinToString("/")
                if (relativePath.isEmpty() || path.fileName.toString() in EXCLUDED_FILES || relativePath in EXCLUDED_PATHS) return@forEach

                val destination = target.resolve(relative.toString())
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination)
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}
