package dev.slne.surf.odyssey.runtime.paper.hub.common.reconnect

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.relativeTo

/** Identifies the map of a hub world by its files. */
object WorldHash {
    private const val BUFFER_SIZE = 64 * 1024

    /**
     * Returns the SHA-256 hash of the map stored in [worldFolder], as lowercase hex.
     *
     * The hash covers `level.dat` in the folder itself and every `.mca` file inside a directory named `region`, at
     * any depth. Files are taken in the order of their paths relative to [worldFolder], and each file contributes
     * its relative path, its size and its content, so the same files give the same hash however they were written,
     * while any changed, added, removed or renamed file gives a different one. Every other file is ignored.
     *
     * @throws java.io.IOException if the folder cannot be read
     */
    fun compute(worldFolder: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        mapFiles(worldFolder).forEach { (relativePath, file) ->
            val pathBytes = relativePath.toByteArray(Charsets.UTF_8)
            digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(pathBytes.size).array())
            digest.update(pathBytes)
            digest.update(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(file.fileSize()).array())
            file.inputStream().use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Returns the [compute] hash of [worldFolder], or `null` if reading the folder fails; the error is passed to
     * [onFailure] once.
     */
    fun computeOrNull(worldFolder: Path, onFailure: (IOException) -> Unit): String? = try {
        compute(worldFolder)
    } catch (exception: IOException) {
        onFailure(exception)
        null
    } catch (exception: UncheckedIOException) {
        onFailure(exception.cause ?: IOException(exception))
        null
    }

    /**
     * Returns the folder named by [configured] if it is a readable directory.
     *
     * @param configured a folder path, or `null` if none is configured
     * @return the folder, or `null` if no path is configured, it is blank or invalid, or it does not name a readable
     *         directory
     */
    fun source(configured: String?): Path? {
        if (configured.isNullOrBlank()) return null
        val folder = runCatching { Path.of(configured) }.getOrNull() ?: return null
        return folder.takeIf { it.isDirectory() && it.isReadable() }
    }

    /** Returns the map files in [worldFolder] with their relative paths, sorted by relative path. */
    private fun mapFiles(worldFolder: Path): List<Pair<String, Path>> = Files.walk(worldFolder).use { paths ->
        paths.filter { it.isRegularFile() && isMapFile(worldFolder, it) }
            .map { it.relativeTo(worldFolder).invariantSeparatorsPathString to it }
            .toList()
    }.sortedBy { it.first }

    /** Returns whether [file] is `level.dat` directly in [worldFolder] or a `.mca` file in a `region` directory. */
    private fun isMapFile(worldFolder: Path, file: Path): Boolean =
        (file.parent == worldFolder && file.name == "level.dat") ||
            (file.extension == "mca" && file.parent?.name == "region")
}
